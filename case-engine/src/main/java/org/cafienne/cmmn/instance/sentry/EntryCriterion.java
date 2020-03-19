package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;

import java.util.List;
import java.util.stream.Collectors;

public class EntryCriterion extends Criterion {
    private final String targetPlanItemName;
    private final Transition targetTransition;

    public EntryCriterion(Stage stage, EntryCriterionDefinition definition) {
        super(stage, definition);
        targetPlanItemName = definition.getTarget();
        targetTransition = definition.getTransition();
    }

    @Override
    protected void satisfy() {
        // Make the list of plan items that can be activated; note, in practice this is only one or zero plan items.
        //  This algoritm may be revised later - it should use PlanItemDefinition (or DiscretionaryItemDefinition), and it can ask the 
        //  stage to provide the single instance being in state Available having the specified Definition (but then Discretionary and PlanItem must be merged in code)
        List<PlanItem> planItemsCurrentlyEligible = stage.getPlanItems().stream().filter(p -> p.getName().equals(targetPlanItemName) && p.getState().equals(State.Available)).collect(Collectors.toList());
        for (PlanItem planItem : planItemsCurrentlyEligible) {
            addDebugInfo(() -> "Entry criterion of '" + planItem + "' is satisfied and will trigger "+targetTransition, this.sentry);
            planItem.repeat();
            planItem.makeTransition(targetTransition);
        }
    }

    @Override
    public boolean isEntryCriterion() {
        return true;
    }
}
