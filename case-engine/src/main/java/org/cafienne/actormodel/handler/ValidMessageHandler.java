package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.UserMessage;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.ModelResponse;

public abstract class ValidMessageHandler extends MessageHandler {

    protected final UserIdentity user;

    /**
     * Valid Messages may lead to a response to the sender.
     */
    protected ModelResponse response = null;

    protected ValidMessageHandler(ModelActor actor, UserMessage msg) {
        super(actor, msg);
        this.user = msg.getUser();
        // NOTE: replace this with some kind of "start transaction"
        this.actor.setCurrentUser((TenantUser) user); // For now always cast to TenantUser
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
