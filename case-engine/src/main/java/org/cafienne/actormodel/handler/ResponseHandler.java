package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.Responder;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.command.exception.AuthorizationException;
import org.cafienne.actormodel.command.response.CommandFailure;
import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.actormodel.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for responses from other model actors.
 * Responses are typically the result of a command that the {@link ModelActor} has sent to another model
 *
 */
public class ResponseHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends ValidMessageHandler<ModelResponse, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(ResponseHandler.class);

    public ResponseHandler(A actor, ModelResponse msg) {
        super(actor, msg);
    }

    /**
     * Runs the case security checks on user context and case tenant.
     */
    @Override
    final protected AuthorizationException runSecurityChecks() {
        return validateUserAndTenant();
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
            } else if (msg instanceof ModelResponse) {
                handler.right.handleResponse(msg);
            }
        }
    }
}
