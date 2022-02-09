package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;

public class ExitCriterion extends Criterion<ExitCriterionDefinition> {
    public ExitCriterion(CriteriaListener listener, ExitCriterionDefinition definition) {
        super(listener, definition);
    }

    @Override
    public boolean isEntryCriterion() {
        return false;
    }
}