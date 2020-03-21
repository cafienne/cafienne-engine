package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;

import java.util.List;
import java.util.stream.Collectors;

public class ExitCriterion extends Criterion {
    private final String targetPlanItemName;
    private final Transition targetTransition;

    public ExitCriterion(Stage stage, ExitCriterionDefinition definition) {
        super(stage, definition);
        targetPlanItemName = definition.getTarget();
        targetTransition = Transition.Exit;
    }

    @Override
    protected void satisfy() {
        // Make a list of the current plan items eligible for the transition.
        // Note: making the copy avoids a ConcurrentModificationException on the stages' plan item collection.
        // Alternatively this collection could have been made CopyOnWriteArrayList, but this is more expensive,
        // and sentry activation is the only place that can cause the modification in a proper way.
        // Of course, if 2 threads simultaneously execute an activity on the case instance, this can also result
        // in the ConcurrentModificationException, however, we do not want to solve that problem inside the engine,
        // but rather outside of it.
        List<PlanItem> planItemsCurrentlyEligible = stage.getPlanItems().stream().filter(p -> p.getName().equals(targetPlanItemName)).collect(Collectors.toList());
        for (PlanItem planItem : planItemsCurrentlyEligible) {
            addDebugInfo(() -> "Exit criterion of '" + planItem + "' is satisfied and will trigger "+targetTransition, this.sentry);
            planItem.makeTransition(targetTransition);
        }
    }
    
    @Override
    public boolean isEntryCriterion() {
        return false;
    }
}