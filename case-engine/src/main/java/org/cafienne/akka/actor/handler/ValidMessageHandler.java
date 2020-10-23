package org.cafienne.akka.actor.handler;

import org.cafienne.akka.actor.MessageHandler;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.TenantUserMessage;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.command.exception.EmptyTenantException;
import org.cafienne.akka.actor.command.exception.InvalidTenantException;
import org.cafienne.akka.actor.event.ModelEvent;

abstract class ValidMessageHandler<M extends TenantUserMessage, C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> extends MessageHandler<M, C, E, A> {

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
}
