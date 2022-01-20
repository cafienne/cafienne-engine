package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.response.CommandFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for messages that are sent to a ModelActor but cannot be handled.
 */
public class InvalidMessageHandler extends MessageHandler {
    private final static Logger logger = LoggerFactory.getLogger(InvalidMessageHandler.class);

    public InvalidMessageHandler(ModelActor actor, Object msg) {
        super(actor, msg);
    }

    protected String errorMsg() {
        return this.actor + " does not support commands of type " + msg.getClass().getName();
    }

    protected void process() {
        if (msg instanceof ModelCommand) {
            // Sent to the wrong type of actor
            ModelCommand invalidCommand = (ModelCommand) msg;
            // Still set the actor, so that it can create a proper failure response.
            invalidCommand.setActor(this.actor);
            Exception wrongCommandType = new Exception(errorMsg());
            addDebugInfo(wrongCommandType::getMessage, logger);
            setResponse(new CommandFailure(invalidCommand, wrongCommandType));
        } else {
            logger.warn(actor + " received a message it cannot handle, of type " + msg.getClass().getName());
        }
    }
}
