package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.Responder;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for responses from other model actors.
 * Responses are typically the result of a command that the {@link ModelActor} has sent to another model
 *
 */
public class ResponseHandler<C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends IncomingMessageHandler<ModelResponse, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(ResponseHandler.class);

    public ResponseHandler(A actor, ModelResponse msg) {
        super(actor, msg);
    }

    protected void process() {
        Responder handler = actor.getResponseListener(msg.getMessageId());
        if (handler == null) {
            // For all commands that are sent to another case via us, a listener is registered.
            // If that listener is null, we set a default listener ourselves.
            // So if we still do not find a listener, it means that we received a response to a command that we never submitted,
            // and we log a warning for that. It basically means someone else has submitted the command and told the other case to respond to us -
            // which is strange.
            logger.warn(actor + " received a response to a message that was not sent through it. Sender: " + actor.sender() + ", response: " + msg);
        } else {
            if (msg instanceof CommandFailure) {
                handler.left.handleFailure((CommandFailure) msg);
            } else {
                handler.right.handleResponse(msg);
            }
        }
    }
}
