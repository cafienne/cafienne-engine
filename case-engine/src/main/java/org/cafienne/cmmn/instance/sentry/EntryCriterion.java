package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItemEntry;

public class EntryCriterion extends Criterion<EntryCriterionDefinition> {
    public EntryCriterion(PlanItemEntry target, EntryCriterionDefinition definition) {
        super(target, definition);
    }

    @Override
    protected void satisfy() {
        target.satisfy(this);
    }

    @Override
    public boolean isEntryCriterion() {
        return true;
    }
}
