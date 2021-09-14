package org.cafienne.actormodel;

import org.cafienne.actormodel.identity.TenantUser;

/**
 * A TenantUserMessage carries a TenantUser
 */
public interface TenantUserMessage {
    TenantUser getUser();
}
