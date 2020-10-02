package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.identity.TenantUser;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around TenantUser, so that user can be accessed from spel expressions
 */
class UserWrapper {
    public final String id;
    public final String name;
    public final Set<String> roles;
    public final String email;

    UserWrapper(TenantUser user) {
        this.id = user.id();
        this.name = user.name();
        this.roles = new HashSet(scala.collection.JavaConverters.seqAsJavaList(user.roles()));
        this.email = user.email();
    }
}
