package org.cafienne.cmmn.expression.spel.api.cmmn.constraint;

import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
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

        // TODO: refactor to CaseFileItemAPI object?

        CaseFileItem item = constraintDefinition.resolveContext(getCase());
        if (constraintDefinition.getContext() != null) {
            super.addPropertyReader(constraintDefinition.getContext().getName(), () -> item.getCurrent().getValue());
        }
        addPropertyReader("caseFileItem", () -> item);

        // TODO: remove, definition is already clear at design time (i.e., when writing the expression itself)
        addPropertyReader("definition", () -> constraintDefinition);
    }

    @Override
    public String getDescription() {
        return constraintDefinition.getContextDescription();
    }
}
