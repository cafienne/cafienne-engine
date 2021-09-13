package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.identity.TenantUser;

public class AkkaSystemMessageHandler<C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends MessageHandler<Object, C, E, A> {

    public AkkaSystemMessageHandler(A actor, Object msg) {
        super(actor, msg, TenantUser.NONE());
    }

    protected void process() {
    }

    protected void complete() {
    }
}
