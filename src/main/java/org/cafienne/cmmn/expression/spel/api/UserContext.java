package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.identity.UserIdentity;

import java.util.ArrayList;

/**
 * Wrapper to access user information from spel expressions
 */
public class UserContext extends APIObject<ModelActor> {
    public UserContext(ModelActor actor, UserIdentity user) {
        super(actor);
        addPropertyReader("id", user::id);
        addDeprecatedReader("roles", ArrayList<String>::new);
        addDeprecatedReader("name", () -> "");
        addDeprecatedReader("email", () -> "");
    }
}