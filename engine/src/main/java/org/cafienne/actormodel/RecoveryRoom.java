/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.actormodel;

import org.apache.pekko.persistence.RecoveryCompleted;
import org.apache.pekko.persistence.SnapshotOffer;
import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.infrastructure.serialization.DeserializationFailure;

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
        } else if (msg instanceof ModelEvent event) {
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
