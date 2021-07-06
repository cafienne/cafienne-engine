package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.actormodel.identity.TenantUser;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper around TenantUser, so that user can be accessed from spel expressions
 */
public class UserContext {
    public final String id;
    public final String name;
    public final Set<String> roles;
    public final String email;

    public UserContext(TenantUser user) {
        this.id = user.id();
        this.name = user.name();
        this.roles = new HashSet<>(scala.jdk.CollectionConverters.SeqHasAsJava(user.roles()).asJava());
        this.email = user.email();
    }
}
