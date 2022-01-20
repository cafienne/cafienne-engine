package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.IncomingActorMessage;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.response.ModelResponse;

public abstract class IncomingMessageHandler extends ValidMessageHandler {
    /**
     * The message that was sent to the actor that is being handled by this handler.
     */
    protected final IncomingActorMessage msg;

    protected IncomingMessageHandler(ModelActor actor, IncomingActorMessage msg) {
        super(actor, msg);
        this.msg = msg;
        // First check the engine version, potentially leading to an extra event.
        checkEngineVersion();
    }

    @Override
    protected boolean hasPersistence() {
        return true;
    }
}
