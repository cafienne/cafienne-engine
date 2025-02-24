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

import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.*;
import org.cafienne.infrastructure.serialization.DeserializationFailure;
import org.cafienne.storage.actormodel.message.StorageEvent;

/**
 * All actor system incoming traffic (either during recovery or upon receiving incoming messages)
 * is passed through the Reception.
 * The reception knows whether the ModelActor is capable of handling the incoming traffic,
 * and when applicable passes it on to the {@link BackOffice}.
 */
class Reception {
    private boolean bootstrapPending = true;
    private final ModelActor actor;
    private boolean isBroken = false;
    private String recoveryFailureInformation = "";
    private boolean isInStorageProcess = false;
    private String actorType = "";
    private final RecoveryRoom recoveryRoom;
    private final BackOffice backoffice;
    final Warehouse warehouse;

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
        switch (message) {
            case IncomingActorMessage visitor -> {
                // Responses are always allowed, as they come only when we have requested something
                if (visitor.isResponse() || canPass(visitor.asCommand())) {
                    backOffice.handleVisitor(visitor);
                }
            }
            case DeserializationFailure failure -> {
                actor.getLogger().error("%s encountered a system error in deserializing a message".formatted(actor), failure);
                // These exceptions mean basically a bug while developing new commands/events/responses.
                // Therefore also log to console.
                failure.exception.printStackTrace(System.out);
            }
            case SnapshotProtocol.Message snapshotMsg -> actor.handleSnapshotProtocolMessage(snapshotMsg); // Weirdly enough snapshotting takes a different route than event persistence...
            case JournalProtocol.Message journalMsg -> actor.handleJournalProtocolMessage(journalMsg);
            case null -> actor.getLogger().warn("{} received a null message from {}", actor, actor.sender());
            default -> actor.getLogger().warn("{} received a message it cannot handle, of type {}", actor, message.getClass().getName());
        }
    }

    private boolean canPass(ModelCommand visitor) {
        // Incoming visitors of type model command need the actor for processing and responding.
        visitor.setActor(actor);

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
                fail(visitor, "Expected bootstrap command in " + actor + " instead of " + visitor.getDescription());
                return false;
            }
        }

        if (!actor.supportsCommand(visitor)) {
            fail(visitor, actor + " does not support commands of type " + visitor.getDescription());
            return false;
        }

        return true;
    }

    void unlock() {
        bootstrapPending = false;
    }

    private void hideFrontdoor(String msg) {
        isBroken = true;
        recoveryFailureInformation = msg;
    }

    private boolean isBroken() {
        return isBroken;
    }

    private boolean informAboutRecoveryFailure(IncomingActorMessage msg) {
        actor.getLogger().warn("Aborting recovery of " + actor + " upon request of type " + msg.getClass().getSimpleName() + " from user " + msg.getUser().id() + ". " + recoveryFailureInformation);
        if (msg.isCommand()) {
            if (msg.isBootstrapMessage()) {
                // Trying to do e.g. StartCase in e.g. a TenantActor
                handleAlreadyCreated(msg);
            } else if (isInStorageProcess) {
                actor.reply(new ActorInStorage(msg.asCommand(), actorType));
            } else {
                String error = actor + " cannot handle message '" + msg.getClass().getSimpleName() + "' because it has not recovered properly. Check the server logs for more details.";
                actor.reply(new ActorChokedFailure(msg.asCommand(), new InvalidCommandException(error)));
            }
            actor.takeABreak("Removing ModelActor[" + actor.getId() + "] because of recovery failure upon unexpected incoming message of type " + msg.getClass().getSimpleName());
        }
        return false;
    }

    private void handleAlreadyCreated(IncomingActorMessage msg) {
        if (msg.isCommand()) {
            actor.reply(new ActorExistsFailure(msg.asCommand(), new IllegalArgumentException("Failure while handling message " + msg.getClass().getSimpleName() + ". Check the server logs for more details")));
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
            hideFrontDoor("Recovery event " + event.getClass().getSimpleName() + " requires an actor of type " + event.actorClass().getSimpleName());
        } else if (event instanceof StorageEvent storageEvent) {
            // Someone is archiving or deleting this actor
            hideFrontdoor("Actor is in storage processing");
            isInStorageProcess = true;
            actorType = storageEvent.metadata().actorType();
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
