package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;

public class EntryCriterion extends Criterion<EntryCriterionDefinition> {
    public EntryCriterion(CriteriaListener listener, EntryCriterionDefinition definition) {
        super(listener, definition);
    }

    @Override
    public boolean isEntryCriterion() {
        return true;
    }
}
