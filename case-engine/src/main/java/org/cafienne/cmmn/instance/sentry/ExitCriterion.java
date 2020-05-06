package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;

public class ExitCriterion extends Criterion<ExitCriterionDefinition> {
    public ExitCriterion(PlanItem target, ExitCriterionDefinition definition) {
        super(target, definition);
    }

    @Override
    protected void satisfy() {
        target.satisfiedExitCriterion(this);
    }

    @Override
    public boolean isEntryCriterion() {
        return false;
    }
}