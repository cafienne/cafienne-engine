package org.cafienne.cmmn.expression.spel.api.cmmn.constraint;

import org.cafienne.cmmn.definition.ApplicabilityRuleDefinition;
import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.instance.PlanItem;

/**
 * Applicability rules are executed on discretionary items related to a Stage or HumanTask.
 * This context provides the additional information with the properties <code>planItem</code> and <code>discretionaryItem</code>.
 */
public class ApplicabilityRuleAPI extends PlanItemRootAPI<ApplicabilityRuleDefinition> {
    public ApplicabilityRuleAPI(PlanItem planItem, DiscretionaryItemDefinition itemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        super(ruleDefinition, planItem);
        addPropertyReader("discretionaryItem", () -> itemDefinition);
    }
}
