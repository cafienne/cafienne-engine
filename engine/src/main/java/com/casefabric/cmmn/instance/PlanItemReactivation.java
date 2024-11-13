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

package com.casefabric.cmmn.instance;

import com.casefabric.cmmn.definition.ItemDefinition;
import com.casefabric.cmmn.definition.sentry.ReactivateCriterionDefinition;
import com.casefabric.cmmn.instance.sentry.CriteriaListener;
import com.casefabric.cmmn.instance.sentry.Criterion;
import com.casefabric.cmmn.instance.sentry.ReactivatingCriterion;
import org.w3c.dom.Element;

public class PlanItemReactivation extends CriteriaListener<ReactivateCriterionDefinition, ReactivatingCriterion> {
    PlanItemReactivation(PlanItem<?> item) {
        super(item, item.getItemDefinition().getReactivatingCriteria());
    }

    @Override
    protected ReactivatingCriterion createCriterion(ReactivateCriterionDefinition definition) {
        return new ReactivatingCriterion(this, definition);
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    @Override
    public void satisfy(Criterion<?> criterion) {
        if (item.getState().isFailed()) {
            item.addDebugInfo(() -> item + " is in Failed state, and " + criterion + " is satisfied and will trigger " + Transition.Reactivate);
            item.makeTransition(Transition.Reactivate);
        } else if (item.getState().isDone()) {
            item.addDebugInfo(() -> item + " is in state " + item.getState() +" so reactivation criteria can be released");
            stopListening();
        }
    }

    public void dumpMemoryStateToXML(Element planItemXML) {
        if (criteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Reactivation criteria "));
        for (Criterion<?> criterion : criteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }

    @Override
    protected void migrateCriteria(ItemDefinition newItemDefinition, boolean skipLogic) {
        migrateCriteria(newItemDefinition.getReactivatingCriteria(), skipLogic);
    }
}
