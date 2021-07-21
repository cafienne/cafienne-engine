/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
}

