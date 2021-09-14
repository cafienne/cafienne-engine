package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.TenantUserMessage;
import org.cafienne.actormodel.command.BootstrapCommand;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.response.CommandFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for messages that are sent to a ModelActor before the ModelActor has received a {@link BootstrapCommand}.
 * @param <C>
 * @param <E>
 * @param <A>
 */
public class NotConfiguredHandler<C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends MessageHandler<Object, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(NotConfiguredHandler.class);

    public NotConfiguredHandler(A actor, TenantUserMessage msg) {
        super(actor, msg, msg.getUser());
    }

    final protected void process() {
    }

    final protected void complete() {
        if (msg instanceof ModelCommand) {
            // Sent to the wrong type of actor
            ModelCommand invalidCommand = (ModelCommand) msg;
            // Still set the actor, so that it can create a proper failure response.
            invalidCommand.setActor(this.actor);
            Exception wrongCommandType = new Exception(this.actor + " cannot handle message '"+msg.getClass().getSimpleName()+"' because it has not been initialized properly");
            addDebugInfo(() -> wrongCommandType.getMessage(), logger);
            CommandFailure response = new CommandFailure((ModelCommand) msg, wrongCommandType);
            actor.reply(response);
        } else {
            logger.warn(actor + " received a message it cannot handle, of type " + msg.getClass().getName());
        }

        actor.persistEvents(events);
    }

}
