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

package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.instance.sentry.CriteriaListener;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.w3c.dom.Element;

public class PlanItemEntry extends CriteriaListener<EntryCriterionDefinition, EntryCriterion> {

    PlanItemEntry(PlanItem<?> item) {
        super(item, item.getItemDefinition().getEntryCriteria());
    }

    @Override
    protected EntryCriterion createCriterion(EntryCriterionDefinition definition) {
        return definition.createInstance(this);
    }

    /**
     * Method invoked by the various state machines when the plan item becomes available;
     * typically determines whether it must be started or should wait for entry criteria to become active
     *
     */
    public void beginLifeCycle() {
        Transition transition = item.getEntryTransition();
        if (criteria.isEmpty()) { // No entry criteria means get started immediately
            item.addDebugInfo(() -> item + ": Starting lifecycle with " + transition + " because there are no entry criteria defined");
            item.makeTransition(transition);
        } else {
            if (earlyBird != null) {
                item.addDebugInfo(() -> item + ": Starting lifecycle with " + transition + " because of " + earlyBird);
                handleCriterionSatisfied(earlyBird);
            } else {
                // Evaluate sentries to see whether one is already active, and, if so, make the transition
                for (Criterion<?> criterion : criteria) {
                    if (criterion.isSatisfied()) {
                        item.addDebugInfo(() -> item + ": an EntryCriterion is satisfied, making transition " + transition);
                        handleCriterionSatisfied(criterion);
                        return;
                    }
                }
                item.addDebugInfo(() -> item + ": Not starting lifecycle with " + transition + " because none of the entry criteria is satisfied");
            }
        }
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    private Criterion<?> earlyBird = null;

    @Override
    public void satisfy(Criterion<?> criterion) {
        if (item.getState().isNull()) {
            // Criterion is an early bird considering our state, let's put it in the waiting room until our lifecycle starts
            earlyBird = criterion;
            return;
        }
        handleCriterionSatisfied(criterion);
    }

    private void handleCriterionSatisfied(Criterion<?> criterion) {
        if (item.getIndex() == 0 && item.getState().isAvailable()) {
            // In this scenario, the entry criterion is triggered on the very first instance of the plan item,
            //  and also for the very first time. Therefore we should not yet repeat, but only make the
            //  entry transition.
            item.addDebugInfo(() -> criterion + " is satisfied and will trigger " + item.getEntryTransition());
            if (this.willNotRepeat()) {
                release();
            }
            item.makeTransition(item.getEntryTransition());
        } else {
            // In all other cases we have to check whether or not to create a repeat item, and, if so,
            //  initiate that with the entry transition
            item.addDebugInfo(() -> criterion + " is satisfied and will repeat " + item);
            release();
            item.repeat("an entry criterion was satisfied");
        }
    }

    private boolean willNotRepeat() {
        return item.getItemDefinition().getPlanItemControl().getRepetitionRule().isDefault();
    }

    public void dumpMemoryStateToXML(Element planItemXML) {
        if (criteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Entry criteria "));
        for (Criterion<?> criterion : criteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }

    @Override
    protected void migrateCriteria(ItemDefinition newItemDefinition) {
        migrateCriteria(newItemDefinition.getEntryCriteria());
        if (criteria.isEmpty()) {
            beginLifeCycle();
        }
    }
}
