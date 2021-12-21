package org.cafienne.actormodel;

import org.cafienne.infrastructure.serialization.CafienneSerializable;

/**
 * An IncomingActorMessage is received by a ModelActor. Typically a ModelCommand or a ModelResponse.
 * It may lead to state changes in the actor
 */
public interface IncomingActorMessage extends CafienneSerializable, UserMessage {
    /**
     * Every message must have a unique identifier. This can be used to correlate Commands and Responses.
     *
     * @return
     */
    String getMessageId();

    /**
     * This method is invoked when handling of the message completed and
     * resulting state changes are to be persisted in the event journal.
     * It can be used by e.g. ModelCommands and ModelResponses to add a {@link org.cafienne.actormodel.event.CommitEvent} event.
     *
     * @return
     */
    void done();
}
