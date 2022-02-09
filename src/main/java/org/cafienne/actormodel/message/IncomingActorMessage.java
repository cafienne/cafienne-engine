package org.cafienne.actormodel.message;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.response.ModelResponse;

/**
 * An IncomingActorMessage is received by a ModelActor. Typically a ModelCommand or a ModelResponse.
 * It may lead to state changes in the actor
 */
public interface IncomingActorMessage extends UserMessage {
    /**
     * Every message must have a unique identifier. This can be used to correlate Commands and Responses.
     *
     * @return
     */
    String getMessageId();

    default boolean isCommand() {
        return false;
    }

    default boolean isResponse() {
        return false;
    }

    default ModelCommand asCommand() {
        return null;
    }

    default ModelResponse asResponse() {
        return null;
    }
}
