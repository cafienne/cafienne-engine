package org.cafienne.actormodel;

import akka.persistence.SnapshotProtocol;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.ActorChokedFailure;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.infrastructure.serialization.DeserializationFailure;

/**
 * All akka incoming traffic (either during recovery or upon receiving incoming messages)
 * is passed through the Reception.
 * The reception knows whether the ModelActor is capable of handling the incoming traffic,
 * and when applicable passes it on to the {@link BackOffice}.
 */
class Reception {
    private boolean bootstrapPending = true;
    private final ModelActor actor;
    private boolean isBroken = false;
    private final RecoveryRoom recoveryRoom;
    private final BackOffice backoffice;
    final Warehouse warehouse;

    private enum Mode {
        Operational,
        Bootstrapped,
        Broken,
    }

    Reception(ModelActor actor) {
        this.actor = actor;
        this.recoveryRoom = new RecoveryRoom(actor, this);
        this.backoffice = new BackOffice(actor, this);
        this.warehouse = new Warehouse(actor);
    }

    void handleRecovery(Object msg) {
        if (isBroken()) {
            // Something has gone wrong with earlier recovery messages, no need to do further processing.
            return;
        }

        recoveryRoom.handleRecovery(msg);
    }

    void handleMessage(Object message) {
        if (message instanceof IncomingActorMessage) {
            IncomingActorMessage visitor = (IncomingActorMessage) message;
            if (canPass(visitor)) {
                backoffice.handleVisitor(visitor);
            }
        } else if (message instanceof SnapshotProtocol.Response) {
            // Weirdly enough snapshotting takes a different route than event persistence...
            actor.handleSnapshotProtocolMessage((SnapshotProtocol.Response) message);
        } else {
            actor.getLogger().warn(this + " received a message it cannot handle, of type " + message.getClass().getName());
        }
    }

    private boolean canPass(IncomingActorMessage visitor) {
        // Note: incoming visitors of type model command need the actor for processing and responding.
        if (visitor.isCommand()) {
            visitor.asCommand().setActor(actor);
        }

        if (isBroken()) {
            return informAboutRecoveryFailure(visitor);
        }

        if (!bootstrapPending && visitor.isBootstrapMessage()) {
            // Cannot run e.g. StartCase two times. Also, we should not reveal it already exists.
            handleAlreadyCreated(visitor);
            return false;
        }

        if (bootstrapPending) {
            if (visitor.isBootstrapMessage()) {
                actor.handleBootstrapMessage(visitor.asBootstrapMessage());
            } else {
                // Cannot run e.g. CompleteHumanTask if the Case is not yet created.
                //  Pretty weird if we get to this stage, as the Routes should not let it come here ...
                fail(visitor, "Expected bootstrap command in " + actor + " instead of " + visitor.getClass().getSimpleName());
                return false;
            }
        }

        if (visitor.isCommand() && !actor.supportsCommand(visitor)) {
            fail(visitor, actor + " does not support commands of type " + visitor.getClass().getSimpleName());
            return false;
        }

        return true;
    }

    void unlock() {
        bootstrapPending = false;
    }

    private void hideFrontdoor(String msg) {
        isBroken = true;
        actor.getLogger().warn("Aborting recovery of " + actor + ". " + msg);
    }

    private boolean isBroken() {
        return isBroken;
    }

    private boolean informAboutRecoveryFailure(IncomingActorMessage msg) {
        if (msg.isCommand()) {
            if (msg.isBootstrapMessage()) {
                // Trying to do e.g. StartCase in e.g. a TenantActor
                handleAlreadyCreated(msg);
            } else {
                String error = actor + " cannot handle message '" + msg.getClass().getSimpleName() + "' because it has not recovered properly. Check the server logs for more details.";
                actor.reply(new ActorChokedFailure(msg.asCommand(), new InvalidCommandException(error)));
            }
        }
        return false;
    }

    private void handleAlreadyCreated(IncomingActorMessage msg) {
        fail(msg, "Failure while handling message " + msg.getClass().getSimpleName() + ". Check the server logs for more details");
    }

    private void fail(IncomingActorMessage message, String errorMessage) {
        if (message.isCommand()) {
            fail(message.asCommand(), errorMessage);
        }
    }

    private void fail(ModelCommand command, String errorMessage) {
        actor.reply(new CommandFailure(command, new InvalidCommandException(errorMessage)));
    }

    void reportDeserializationFailure(DeserializationFailure failure) {
        hideFrontdoor("" + failure);
    }

    void reportInvalidRecoveryEvent(ModelEvent event) {
        if (event.isBootstrapMessage()) {
            // Wrong type of ModelActor. Probably someone tries to re-use the same actor id for another type of ModelActor.
            hideFrontdoor("Recovery event " + event.getClass().getSimpleName() + " requires an actor of type " + event.actorClass().getSimpleName());
        } else {
            // Pretty weird if it happens ...
            hideFrontdoor("Received unexpected recovery event of type " + event.getClass().getName());
        }
    }

    void reportStateUpdateFailure(Throwable throwable) {
        actor.getLogger().error("Unexpected error during recovery of " + actor, throwable);
        hideFrontdoor("Updating actor state failed.");
    }

    void open() {
        actor.recoveryCompleted();
    }
}