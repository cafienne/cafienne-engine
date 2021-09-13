package org.cafienne.actormodel;

import org.cafienne.actormodel.identity.TenantUser;

/**
 * A UserMessage carries user information
 * Typically used in Commands and Events that result from those commands.
 */
public interface UserMessage {
    TenantUser getUser();
}
