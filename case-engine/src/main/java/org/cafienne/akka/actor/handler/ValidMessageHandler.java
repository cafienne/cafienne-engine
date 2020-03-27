package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.TenantUserMessage;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.event.ModelEvent;

abstract class ValidMessageHandler<M extends TenantUserMessage, C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<M, C, E, A> {

    protected ValidMessageHandler(A actor, M msg) {
        super(actor, msg, msg.getUser());
    }
}
