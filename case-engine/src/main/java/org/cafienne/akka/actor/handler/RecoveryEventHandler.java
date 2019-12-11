package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryEventHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<E, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(RecoveryEventHandler.class);

    public RecoveryEventHandler(A actor, E msg) {
        super(actor, msg);
    }

    @Override
    final protected InvalidCommandException runSecurityChecks() {
        return null;
    }

    protected void process() {
    }

    protected void complete() {
    }

    @Override
    protected TenantUser getUser() {
        return msg.getUser();
    }
}
