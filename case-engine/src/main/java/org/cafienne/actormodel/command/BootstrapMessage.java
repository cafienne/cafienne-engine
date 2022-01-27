package org.cafienne.actormodel.command;

import org.cafienne.actormodel.message.UserMessage;

/**
 * The first command that is sent to a ModelActor has to implement this interface such that the actor can
 * initialize itself with the required information.
 * This is required to enable the ModelActor class to do some basic authorization checks that must be done by
 * the platform and cannot be left to actor specific logic overwriting it.
 * It should also be implemented in the first state-changing ModelEvent, so that the same information can be set
 * during recovery of the ModelActor
 */
public interface BootstrapMessage extends UserMessage {
    String tenant();

    @Override
    default boolean isBootstrapMessage() {
        return true;
    }
}
