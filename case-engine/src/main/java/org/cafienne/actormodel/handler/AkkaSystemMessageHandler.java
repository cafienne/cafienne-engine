package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;

public class AkkaSystemMessageHandler extends MessageHandler {

    public AkkaSystemMessageHandler(ModelActor actor, Object msg) {
        super(actor, msg);
    }
}
