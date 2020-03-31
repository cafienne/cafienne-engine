package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.Responder;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.exception.MissingTenantException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Handler for responses from other model actors.
 * Responses are typically the result of a command that the {@link ModelActor} has sent to another model
 *
 */
public class ResponseHandler<C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends ValidMessageHandler<ModelResponse, C, E, A> {
    private final static Logger logger = LoggerFactory.getLogger(ResponseHandler.class);
    private final TenantUser user;

    public ResponseHandler(A actor, ModelResponse msg) {
        super(actor, msg);
        this.user = msg.getUser();
    }

    /**
     * Runs the case security checks on user context and case tenant.
     */
    @Override
    final protected InvalidCommandException runSecurityChecks() {
        InvalidCommandException issue = null;
        // Security checks:
        // - need a proper user.
        // - is the case tenant already available, and is the user in that same tenant?
        // Same exception is needed for security reasons: do not expose more info than needed.
        if (user == null || user.id() == null || user.id().trim().isEmpty()) {
            issue = new InvalidCommandException("The user id must not be null or empty");
        } else if (user.tenant() == null) {
            issue = new MissingTenantException("User must provide tenant information in order to execute a command");
        } else if (actor.getTenant() != null && !actor.getTenant().equals(user.tenant())) {
            issue = new InvalidCommandException("Cannot handle command " + getClass().getName() + "; StartCase not yet sent, or not in this user context");
        }

        return issue;
    }

    protected void process() {
        Responder handler = actor.getResponseListener(msg.getMessageId());
        if (handler == null) {
            // For all commands that are sent to another case via us, a listener is registered.
            // If that listener is null, we set a default listener ourselves.
            // So if we still do not find a listener, it means that we received a response to a command that we never submitted,
            // and we log a warning for that. It basically means someone else has submitted the command and told the other case to respond to us -
            // which is strange.
            logger.warn(actor.getClass().getSimpleName() + " " + actor.getId() + " received a response to a message that was not sent through it. Sender: " + actor.sender() + ", response: " + msg);
        } else {
            if (msg instanceof CaseResponse) {
                handler.right.handleResponse((CaseResponse) msg);
            } else if (msg instanceof CommandFailure) {
                handler.left.handleFailure((CommandFailure) msg);
            }
        }
    }

    protected void complete() {
        // First check the engine version.
        checkEngineVersion();

        // If there are only debug events, then were done, otherwise add a last modified event.
        if (!events.isEmpty()) {

            // We have events to persist, but let's check if it is only debug events or more.
            if (! hasOnlyDebugEvents()) {
                // Change the last modified moment of this actor and publish an event about it
                addEvent(actor.createLastModifiedEvent(Instant.now()));
            }

            // Now persist the events in one shot
            actor.persistEvents(events);
        }
    }
}
