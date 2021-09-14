package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.TenantUserMessage;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.response.ModelResponse;

public abstract class ValidMessageHandler<M extends TenantUserMessage, C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends MessageHandler<M, C, E, A> {
    /**
     * Valid Messages may lead to a response to the sender.
     */
    protected ModelResponse response = null;

    protected ValidMessageHandler(A actor, M msg) {
        super(actor, msg, msg.getUser());
    }

    protected void complete() {
        // First check the engine version.
        checkEngineVersion();

        // If there are only debug events, first respond and then persist the events (for performance).
        // Otherwise, only send a response upon successful persisting the events.
        if (hasOnlyDebugEvents()) {
            actor.replyAndThenPersistEvents(events, response);
        } else {
            // Inform the message that we're done handling it. Can typically be used to add a transaction event
            actor.persistEventsAndThenReply(events, response);
        }
    }
}
