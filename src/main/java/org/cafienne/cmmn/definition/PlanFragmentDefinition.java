/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.sentry.SentryDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public abstract class PlanFragmentDefinition extends PlanItemDefinitionDefinition {
    private final static Logger logger = LoggerFactory.getLogger(PlanFragmentDefinition.class);

    private final Collection<PlanItemDefinition> planItems = new ArrayList<>();
    private final Collection<SentryDefinition> sentries = new ArrayList<>();

    public PlanFragmentDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        parse("planItem", PlanItemDefinition.class, planItems);
        parse("sentry", SentryDefinition.class, sentries);
    }

    public Collection<SentryDefinition> getSentries() {
        return sentries;
    }

    public SentryDefinition getSentry(String identifier) {
        return sentries.stream().filter(s -> s.getName().equals(identifier) || s.getId().equals(identifier)).findFirst().orElse(null);
    }

    public Collection<PlanItemDefinition> getPlanItems() {
        return planItems;
    }

    /**
     * Retrieve the definition of the plan item with the specified identifier, or null if no plan item with this id is available within this PlanFragment or Stage
     *
     * @param planItemId
     * @return
     */
    public PlanItemDefinition getPlanItem(String planItemId) {
        return planItems.stream().filter(p -> p.getId().equals(planItemId) || p.getName().equals(planItemId)).findFirst().orElse(null);
    }

    /**
     * Search for a plan item in this planFragment (stage) or in its children.
     *
     * @param planItemId
     * @return
     */
    PlanItemDefinition searchPlanItem(String planItemId) {
        // First, see if we have it in this planFragment; if not, search in our children.
        PlanItemDefinition item = getPlanItem(planItemId);
        if (item != null) {
            return item;
        }

        // So it is not available at our level; let's search our children.
        for (PlanItemDefinition childPlanItem : planItems) {
            PlanItemDefinitionDefinition definition = childPlanItem.getPlanItemDefinition();
            if (definition == null) {
                logger.error("Wow... the definition of " + childPlanItem.getId() + " is missing while searching for planItem with id " + planItemId);

            }
            // System.out.println("Encountered: "+definition.getClass().getSimpleName());
            if (definition instanceof PlanFragmentDefinition) {
                item = ((PlanFragmentDefinition) definition).searchPlanItem(planItemId);
                if (item != null) {
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public PlanItem<?> createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Start;
    }

    public boolean samePlanItems(PlanFragmentDefinition other) {
        return same(planItems, other.planItems);
    }

    public boolean sameSentries(PlanFragmentDefinition other) {
        return same(sentries, other.sentries);
    }

    public boolean samePlanFragment(PlanFragmentDefinition other) {
        return samePlanItemDefinitionDefinition(other)
                && samePlanItems(other)
                && sameSentries(other);
    }
}
