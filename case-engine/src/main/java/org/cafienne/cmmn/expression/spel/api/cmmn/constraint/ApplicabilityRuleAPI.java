package org.cafienne.cmmn.expression.spel.api.cmmn.constraint;

import org.cafienne.cmmn.definition.ApplicabilityRuleDefinition;
import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.instance.PlanItem;

/**
 * Applicability rules are executed on discretionary items related to a Stage or HumanTask.
 * This context provides the additional information with the properties <code>planItem</code> and <code>discretionaryItem</code>.
 */
public class ApplicabilityRuleAPI extends PlanItemRootAPI<ApplicabilityRuleDefinition> {
    private final ApplicabilityRuleDefinition ruleDefinition;
    private final DiscretionaryItemDefinition itemDefinition;

    public ApplicabilityRuleAPI(PlanItem<?> planItem, DiscretionaryItemDefinition itemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        super(ruleDefinition, planItem);
        this.ruleDefinition = ruleDefinition;
        this.itemDefinition = itemDefinition;
        addPropertyReader("discretionaryItem", () -> itemDefinition);
    }

    @Override
    public String getDescription() {
        return "applicability rule '" + ruleDefinition.getName() + "' for discretionary item " + itemDefinition;
    }
}
