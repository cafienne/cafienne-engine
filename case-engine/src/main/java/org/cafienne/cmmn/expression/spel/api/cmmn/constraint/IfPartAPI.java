package org.cafienne.cmmn.expression.spel.api.cmmn.constraint;

import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.instance.sentry.Criterion;

/**
 * Context for evaluation of an if part in a criterion.
 */
public class IfPartAPI extends PlanItemRootAPI<IfPartDefinition> {
    public IfPartAPI(IfPartDefinition ifPartDefinition, Criterion criterion) {
        super(ifPartDefinition, criterion.getTarget());
        // TODO: remove, which criterion it is, is already clear during design time
        addPropertyReader("criterion", () -> criterion);
        addDeprecatedReader("sentry", "criterion", () -> criterion); // Compatibility
    }

    @Override
    public String getDescription() {
        return "ifPart in sentry";
    }
}
