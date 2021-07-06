package org.cafienne.actormodel;

import org.cafienne.actormodel.event.TransactionEvent;
import org.cafienne.actormodel.identity.TenantUser;

/**
 * A TenantUserMessage carries a TenantUser
 */
public interface TenantUserMessage<T extends ModelActor<?,?>> {
    TenantUser getUser();

    /**
     * Return an (optional) TransactionEvent for this message. This logic is invoked when the actor
     * has created state changing events during handling of the message.
     * Defaults to asking the ModelActor to create the event, but it may not be required for certain
     * types of commands (e.g. a PlatformUpdate on user id's). In that case, the command should return null.
     * @param actor
     * @return
     */
    default TransactionEvent<T> createTransactionEvent(T actor) {
        return actor.createTransactionEvent();
    }
}
