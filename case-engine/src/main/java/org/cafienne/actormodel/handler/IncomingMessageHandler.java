package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.IncomingActorMessage;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.EmptyTenantException;
import org.cafienne.actormodel.exception.InvalidTenantException;
import org.cafienne.actormodel.response.ModelResponse;

public abstract class IncomingMessageHandler<M extends IncomingActorMessage, C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends ValidMessageHandler<M, C, E, A> {
    /**
     * Valid Messages may lead to a response to the sender.
     */
    protected ModelResponse response = null;

    protected IncomingMessageHandler(A actor, M msg) {
        super(actor, msg);
    }

    protected AuthorizationException validateUserAndTenant() {
        // Security checks:
        // - need a proper user.
        // - is the case tenant already available, and is the user in that same tenant?
        // Same exception is needed for security reasons: do not expose more info than needed.
        if (this.user == null || user.id() == null || user.id().trim().isEmpty()) {
            // Note: this check is also done in ModelCommand itself
            return new AuthorizationException("The user id must not be null or empty");
        } else if (user.tenant() == null) {
            // Note: this check is also done in ModelCommand itself
            return new EmptyTenantException("User must provide tenant information in order to execute a command");
        } else if (actor.getTenant() != null && !actor.getTenant().equals(user.tenant())) {
            // Note: this check can only be done here
            return new InvalidTenantException(user, msg, actor);
        }
        return null;
    }

    protected void complete() {
        // First check the engine version.
        checkEngineVersion();

        // If there are only debug events, first respond and then persist the events (for performance).
        // Otherwise, only send a response upon successful persisting the events.
        if (hasOnlyDebugEvents()) {
            actor.replyAndThenPersistEvents(events, response);
        } else {
            // Inform the message that we're done handling it. Can typically be used to add a transaction event
            msg.done();
            actor.persistEventsAndThenReply(events, response);
        }
    }
}
