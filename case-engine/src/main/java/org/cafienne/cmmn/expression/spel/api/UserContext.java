package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.TenantUser;

import java.util.ArrayList;

/**
 * Wrapper around TenantUser, so that user can be accessed from spel expressions
 */
public class UserContext extends APIObject<ModelActor<?, ?>> {
    public UserContext(ModelActor<?, ?> actor, TenantUser user) {
        super(actor);
        addPropertyReader("id", user::id);
        addDeprecatedReader("roles", ArrayList<String>::new);
        addDeprecatedReader("name", () -> "");
        addDeprecatedReader("email", () -> "");
    }
}
