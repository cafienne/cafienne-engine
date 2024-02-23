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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.DiscretionaryItem;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class PlanningTableDefinition extends TableItemDefinition {
    private final Collection<TableItemDefinition> tableItems = new ArrayList<>();
    private final Collection<ApplicabilityRuleDefinition> ruleDefinitions = new ArrayList<>();

    public PlanningTableDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        parse("discretionaryItem", DiscretionaryItemDefinition.class, tableItems);
        parse("planningTable", PlanningTableDefinition.class, tableItems);
        parse("applicabilityRule", ApplicabilityRuleDefinition.class, ruleDefinitions);
    }

    /**
     * Returns the definition of the applicability rule with the specified name, or null if the rule with the name is not present in this planning table
     *
     * @param identifier
     * @return
     */
    ApplicabilityRuleDefinition getApplicabilityRule(String identifier) {
        return ruleDefinitions.stream().filter(s -> s.getName().equals(identifier) || s.getId().equals(identifier)).findFirst().orElse(null);
    }

    @Override
    public Element dumpMemoryStateToXML(Element parentElement, Stage<?> stage) {
        Element planningTableXML = parentElement.getOwnerDocument().createElement("PlanningTable");
        parentElement.appendChild(planningTableXML);

        // Print roles
        super.dumpMemoryStateToXML(planningTableXML, stage);

        // Print table items
        for (TableItemDefinition tableItem : tableItems) {
            tableItem.dumpMemoryStateToXML(planningTableXML, stage);
        }

        return planningTableXML;
    }

    /**
     * Indicates whether or not there are instances of DiscretionaryItem available within this table;
     *
     * @return
     */
    public boolean hasItems(PlanItem<?> containingPlanItem) {
        // We cannot have discretionary items if our containing plan item is not in a state that allows planning; e.g., Completed human tasks or stages do NOT have discretionary items
        if (!isPlanningAllowed(containingPlanItem)) {
            return false;
        }
        for (TableItemDefinition item : tableItems) {
            if (item instanceof DiscretionaryItemDefinition) {
                return true;
            } else if (item instanceof PlanningTableDefinition) {
                if (((PlanningTableDefinition) item).hasItems(containingPlanItem)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void evaluate(PlanItem<?> containingPlanItem, Collection<DiscretionaryItem> items) {
        if (isPlanningAllowed(containingPlanItem)) {
            tableItems.forEach(t -> t.evaluate(containingPlanItem, items));
        }
    }

    @Override
    protected DiscretionaryItemDefinition getDiscretionaryItem(String identifier) {
        for (TableItemDefinition item : tableItems) {
            DiscretionaryItemDefinition diDefinition = item.getDiscretionaryItem(identifier);
            if (diDefinition != null) {
                return diDefinition;
            }
        }
        return null;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::samePlanningTable);
    }

    public boolean samePlanningTable(PlanningTableDefinition other) {
        return sameTableItem(other)
                && same(tableItems, other.tableItems)
                && same(ruleDefinitions, other.ruleDefinitions);
    }
}
