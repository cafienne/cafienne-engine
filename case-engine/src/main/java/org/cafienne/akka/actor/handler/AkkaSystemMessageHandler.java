package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AkkaSystemMessageHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<Object, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(AkkaSystemMessageHandler.class);

    public AkkaSystemMessageHandler(A actor, Object msg) {
        super(actor, msg, TenantUser.NONE());
    }

    @Override
    final protected InvalidCommandException runSecurityChecks() {
        return null;
    }

    protected void process() {
    }

    protected void complete() {
    }
}
