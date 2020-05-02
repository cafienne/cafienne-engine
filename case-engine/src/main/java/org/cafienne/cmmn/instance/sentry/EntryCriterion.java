package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;

public class EntryCriterion extends Criterion<EntryCriterionDefinition> {
    public EntryCriterion(PlanItem target, EntryCriterionDefinition definition) {
        super(target, definition);
    }

    @Override
    protected void satisfy() {
        target.satisfiedEntryCriterion(this);
    }

    @Override
    public boolean isEntryCriterion() {
        return true;
    }
}
