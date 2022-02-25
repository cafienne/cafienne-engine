package org.cafienne.actormodel;

import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.infrastructure.serialization.DeserializationFailure;
import org.cafienne.infrastructure.serialization.Fields;

/**
 * The RecoveryRoom is where the ModelActor is brought back into memory upon reception of new incoming traffic.
 * Failures during recovery are reported to the {@link Reception}
 */
class RecoveryRoom {
    private final ModelActor actor;
    private final Reception reception;

    RecoveryRoom(ModelActor actor, Reception reception) {
        this.actor = actor;
        this.reception = reception;
        if (actor.getLogger().isDebugEnabled()) {
            actor.getLogger().debug("Opening recovery of " + actor);
        }
    }

    void handleRecovery(Object msg) {
        // Steps:
        // 0. Pass snapshots to the model actor
        // 1. For ModelEvents:
        //  a. Ignore DebugEvent message
        //  b. If supported event, run the recovery handler for it, including necessary bootstrapping
        //  c. If unsupported event indicate inconsistent recovery (probably upcoming IncomingMessage needs a different type of ModelActor)
        // 2. DeserializationFailures indicate inconsistent recovery
        // 3. Invoke a "hook" method to indicate recovery completion upon RecoveryCompleted (used in TimerService)
        // 4. In all other cases print warn statements and ignore the event.
        if (msg instanceof SnapshotOffer) {
            actor.handleSnapshot((SnapshotOffer) msg);
        } else if (msg instanceof ModelEvent) {
            ModelEvent event = (ModelEvent) msg;
            actor.setCurrentUser(event.getUser());
            if (event instanceof DebugEvent) {
                // Step 1a, ignore debug events
            } else if (actor.supportsEvent(event) || event instanceof EngineVersionChanged) {
                // Step 1b, supported event
                recoverEvent(event);
            } else {
                // Step 1c. Weird: ModelEvents in recovery of other models??
                reception.reportInvalidRecoveryEvent(event);
            }
        } else if (msg instanceof DeserializationFailure) {
            // Step 2. Probably incompatible change in event serialization format. Big issue
            reception.reportDeserializationFailure((DeserializationFailure) msg);
        } else if (msg instanceof RecoveryCompleted) {
            // Step 3.
            if (actor.getLogger().isDebugEnabled()) {
                actor.getLogger().debug(actor + " completed recovery");
            }
            reception.open();
        } else {
            // Step 4.
            actor.getLogger().warn(actor + " received unknown message of type " + msg.getClass().getName() + " during recovery: " + msg);
        }
    }

    private void recoverEvent(ModelEvent event) {
        if (event.isBootstrapMessage()) {
            // Set the tenant.
            // TODO: refactor this later on.
            //  The code sets the tenant, but not all actors belong to a tenant.
            //  Hence, it is much better if the bootstrap event
            actor.handleBootstrapMessage(event.asBootstrapMessage());
        }
//        System.out.println("Recovering event " + actor + ".[" + actor.lastSequenceNr()+ "].[" +  event.getTimestamp().toString().substring(0, 23) +"].[" + event.getClass().getSimpleName()+ "]");
        if (actor.getLogger().isDebugEnabled()) {
            actor.getLogger().debug("Recovering event " + actor + ".[" + actor.lastSequenceNr()+ "].[" +  event.getTimestamp().toString().substring(0, 23) +"].[" + event.getClass().getSimpleName()+ "]");
        }
        try {
            event.updateActorState(actor);
            if (event.isBootstrapMessage()) {
                reception.unlock();
            }
        } catch (Throwable throwable) {
            reception.reportStateUpdateFailure(throwable);
        }
    }
}
