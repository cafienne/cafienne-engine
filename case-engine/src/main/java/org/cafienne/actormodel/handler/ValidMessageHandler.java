package org.cafienne.actormodel.handler;

import org.cafienne.actormodel.MessageHandler;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.TenantUserMessage;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.EmptyTenantException;
import org.cafienne.actormodel.exception.InvalidTenantException;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.event.TransactionEvent;

abstract class ValidMessageHandler<M extends TenantUserMessage, C extends ModelCommand<A>, E extends ModelEvent<A>, A extends ModelActor<C, E>> extends MessageHandler<M, C, E, A> {
    /**
     * Valid Messages may lead to a response to the sender.
     */
    protected ModelResponse response = null;

    protected ValidMessageHandler(A actor, M msg) {
        super(actor, msg, msg.getUser());
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

    /**
     * Optionally create and add a transaction event at the end of handling this message
     * @return
     */
    protected TransactionEvent createTransactionEvent() {
        TransactionEvent transactionEvent = msg.createTransactionEvent(actor);
        if (transactionEvent != null) {
            transactionEvent.setCause(msg.getClass().getName());
            addModelEvent(transactionEvent);
        }
        return transactionEvent;
    }

    protected void complete() {
        // First check the engine version.
        checkEngineVersion();

        // If there are only debug events, first respond and then persist the events (for performance).
        // Otherwise, only send a response upon successful persisting the events.
        if (hasOnlyDebugEvents()) {
            actor.replyAndThenPersistEvents(events, response);
        } else {
            // Tell the actor to (optionally) add a transaction event
            createTransactionEvent();
            actor.persistEventsAndThenReply(events, response);
        }
    }
}
