package org.cafienne.akka.actor;

import org.cafienne.akka.actor.identity.TenantUser;

/**
 * A TenantUserMessage carries a TenantUser
 */
public interface TenantUserMessage {
    TenantUser getUser();
}
