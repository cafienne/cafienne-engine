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

package org.cafienne.engine.cmmn.instance;

import org.cafienne.engine.cmmn.definition.ItemDefinition;
import org.cafienne.engine.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.engine.cmmn.instance.sentry.CriteriaListener;
import org.cafienne.engine.cmmn.instance.sentry.Criterion;
import org.cafienne.engine.cmmn.instance.sentry.ExitCriterion;
import org.w3c.dom.Element;

public class PlanItemExit extends CriteriaListener<ExitCriterionDefinition, ExitCriterion> {
    PlanItemExit(PlanItem<?> item) {
        super(item, item.getItemDefinition().getExitCriteria());
    }

    @Override
    protected ExitCriterion createCriterion(ExitCriterionDefinition definition) {
        return new ExitCriterion(this, definition);
    }

    @Override
    public void satisfy(Criterion<?> criterion) {
        item.addDebugInfo(() -> criterion + " is satisfied, triggering exit on " + item);
        stopListening();
        item.makeTransition(item.getExitTransition());
    }

    void dumpMemoryStateToXML(Element planItemXML) {
        if (criteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Exit criteria "));
        for (Criterion<?> criterion : criteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }

    @Override
    protected void migrateCriteria(ItemDefinition newItemDefinition, boolean skipLogic) {
        migrateCriteria(newItemDefinition.getExitCriteria(), skipLogic);
    }
}
