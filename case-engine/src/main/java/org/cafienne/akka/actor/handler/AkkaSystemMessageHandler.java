package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;

public class AkkaSystemMessageHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<Object, C, E, A> {

    public AkkaSystemMessageHandler(A actor, Object msg) {
        super(actor, msg, TenantUser.NONE());
    }

    protected void process() {
    }

    protected void complete() {
    }
}
