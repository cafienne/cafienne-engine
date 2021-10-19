package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.IncomingActorMessage;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.response.ModelResponse;

public abstract class IncomingMessageHandler extends ValidMessageHandler {
    /**
     * The message that was sent to the actor that is being handled by this handler.
     */
    protected final IncomingActorMessage msg;

    /**
     * Valid Messages may lead to a response to the sender.
     */
    protected ModelResponse response = null;

    protected IncomingMessageHandler(ModelActor actor, IncomingActorMessage msg) {
        super(actor, msg);
        this.msg = msg;
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
            msg.done();
            actor.persistEventsAndThenReply(events, response);
        }
    }
}
