package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.UserMessage;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.ModelResponse;

public abstract class ValidMessageHandler extends MessageHandler {

    protected final UserIdentity user;

    protected ValidMessageHandler(ModelActor actor, UserMessage msg) {
        super(actor, msg);
        this.user = msg.getUser();
        // NOTE: replace this with some kind of "start transaction"
        this.actor.setCurrentUser(user);
    }
}
