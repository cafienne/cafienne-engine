package org.cafienne.cmmn.expression.spel.api.cmmn.constraint;

import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.cmmn.expression.spel.api.cmmn.file.CaseFileItemAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.file.ValueAPI;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

/**
 * Context of current plan item. Can be referred to by it's type (task, stage, milestone or event), or by plain "planitem".
 * Used for evalution of item control rules (required, repetition, manual activation), and for
 * custom HumanTask settings on Assignment and DueDate.
 */
public class PlanItemRootAPI<T extends ConstraintDefinition> extends CaseRootObject {


    private final T constraintDefinition;

    public PlanItemRootAPI(T constraintDefinition, PlanItem planItem) {
        super(planItem.getCaseInstance());
        this.constraintDefinition = constraintDefinition;

        // Make sure we can directly access the task or stage or milestone; e.g. "task.index < 3"
        registerPlanItem(planItem);

        if (constraintDefinition.getContext() != null) {
            CaseFileItem item = constraintDefinition.resolveContext(getCase());
            // By name we return the case file item's JSON structure
            super.addPropertyReader(constraintDefinition.getContext().getName(), () -> new ValueAPI(item.getCurrent()));
            // Also add the API reference to the case file item object itself
            addPropertyReader("caseFileItem", () -> new CaseFileItemAPI(item));
        }
    }

    @Override
    public String getDescription() {
        return constraintDefinition.getContextDescription();
    }
}
