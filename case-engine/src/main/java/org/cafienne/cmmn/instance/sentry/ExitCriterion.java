package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.PlanItemExit;

public class ExitCriterion extends Criterion<ExitCriterionDefinition> {
    public ExitCriterion(PlanItemExit target, ExitCriterionDefinition definition) {
        super(target, definition);
    }

    @Override
    protected void satisfy() {
        target.satisfy(this);
    }

    @Override
    public boolean isEntryCriterion() {
        return false;
    }
}