package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.instance.Case;

import java.util.HashSet;
import java.util.Set;

/**
 * Base context for SPEL expressions, enabling access to the case and it's public members from any expression.
 * <p>Some example expressions:
 * <ul>
 * <li><code>caseInstance.id</code> - The id of the case</li>
 * <li><code>user.id</code> - The unique id of the user executing the current command in the case</li>
 * <li><code>caseInstance.planItems.size()</code> - The number of plan items currently in the case</li>
 * <li><code>caseInstance.definition.name</code> - The name of case definition</li>
 * <li><code>caseInstance.definition.caseRoles</code> - The roles defined in the case</li>
 * </ul>
 * 
 * See {@link Case} itself for it's members.
 */
abstract class ExpressionContext implements SpelReadable {
    public final Object caseInstance;
    public final ModelActor model;
    public final UserWrapper user;

    // Perhaps extend later with other information, such as CaseTeam or current user?
    // but for now, caseInstance is sufficient as a starting path to fetch any required information

    protected ExpressionContext(ModelActor model) {
        this.model = model;
        this.caseInstance = model;
        this.user = new UserWrapper(model.getCurrentUser());
    }
}

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