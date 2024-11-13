/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.definition.sentry;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.DiscretionaryItemDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.definition.PlanItemDefinition;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.instance.sentry.CriteriaListener;
import com.casefabric.cmmn.instance.sentry.EntryCriterion;
import org.w3c.dom.Element;

public class EntryCriterionDefinition extends CriterionDefinition {
    /**
     * The transition to be invoked when this criterion becomes active.
     * Milestones and EventListeners trigger {@link Transition#Occur}.
     * Task and Stages trigger {@link Transition#Start}
     */
    private Transition entryTransition;

    public EntryCriterionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    /**
     * Only after resolving sentries we know what transition we need to make
     */
    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        CMMNElementDefinition parent = getParentElement();
        if (parent instanceof PlanItemDefinition) {
            entryTransition = ((PlanItemDefinition) parent).getPlanItemDefinition().getEntryTransition();
        } else if (parent instanceof DiscretionaryItemDefinition) {
            entryTransition = ((DiscretionaryItemDefinition) parent).getPlanItemDefinition().getEntryTransition();
        } else {
            getCaseDefinition().addReferenceError(getContextDescription() + "Found an entry criterion inside a " + parent.getClass().getSimpleName() + ", but that type is not supported for entry criteria");
        }
    }

    /**
     * Returns true if there is at least one on part in this definition
     */
    public boolean hasOnParts() {
        // Check whether sentry definition exists at all, and if so, check whether it has on parts.
        return this.getSentryDefinition() != null && !getSentryDefinition().getOnParts().isEmpty();
    }

    @Override
    public Transition getTransition() {
        return entryTransition;
    }
}
