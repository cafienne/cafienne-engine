package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.CasePlanExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;

public class CasePlanExitCriterion extends ExitCriterion {
    private final PlanItem casePlan;
    
    public CasePlanExitCriterion(Stage stage, CasePlanExitCriterionDefinition definition) {
        super(stage, definition);
        this.casePlan = stage;
    }
    
    @Override
    protected void satisfy(OnPart<?, ?> activator) {
        addDebugInfo(() -> "Case plan exit criterion is satisfied and will terminate the plan", this.sentry);
        casePlan.makeTransition(Transition.Terminate);
    }
}
