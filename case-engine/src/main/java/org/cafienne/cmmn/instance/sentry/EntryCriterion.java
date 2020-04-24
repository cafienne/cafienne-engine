package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;

public class EntryCriterion extends Criterion<EntryCriterionDefinition> {
    private final Transition targetTransition;
    private boolean satisfied;
    private PlanItem nextToRepeat;

    public EntryCriterion(Stage stage, EntryCriterionDefinition definition) {
        super(stage, definition);
        targetTransition = definition.getTransition();
    }

    public void addPlanItem(PlanItem planItem) {
        this.nextToRepeat = planItem;
        if (satisfied) {
            trigger();
        }
    }

    @Override
    protected void satisfy(OnPart<?, ?> activator) {
        satisfied = true;
        trigger();
    }

    private int triggerCount = 0;

    private void trigger() {
        triggerCount ++;
        if (nextToRepeat != null) {
            addDebugInfo(() -> this + " is satisfied("+triggerCount+") and will trigger "+targetTransition);
            satisfied = false;
            nextToRepeat.trigger(targetTransition);
        }
    }

    @Override
    public boolean isEntryCriterion() {
        return true;
    }
}
