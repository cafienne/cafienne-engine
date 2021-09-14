package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.response.CommandFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidMessageHandler<C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends MessageHandler<Object, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(InvalidMessageHandler.class);

    public InvalidMessageHandler(A actor, Object msg) {
        super(actor, msg, TenantUser.NONE());
    }

    protected void process() {
    }

    protected void complete() {
        if (msg instanceof ModelCommand) {
            // Sent to the wrong type of actor
            ModelCommand invalidCommand = (ModelCommand) msg;
            // Still set the actor, so that it can create a proper failure response.
            invalidCommand.setActor(this.actor);
            Exception wrongCommandType = new Exception(this.actor + " does not support commands of type " + invalidCommand.getClass().getName());
            addDebugInfo(() -> wrongCommandType.getMessage(), logger);
            CommandFailure response = new CommandFailure((ModelCommand) msg, wrongCommandType);
            actor.reply(response);
        } else {
            logger.warn(actor + " received a message it cannot handle, of type " + msg.getClass().getName());
        }

        actor.persistEvents(events);
    }

}
