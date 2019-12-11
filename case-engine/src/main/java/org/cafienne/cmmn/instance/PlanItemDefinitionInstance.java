/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.util.Collection;

import org.cafienne.cmmn.definition.PlanItemDefinitionDefinition;
import org.w3c.dom.Element;

public abstract class PlanItemDefinitionInstance<T extends PlanItemDefinitionDefinition> extends CMMNElement<T> {
    private final PlanItem planItem;

    protected PlanItemDefinitionInstance(PlanItem planItem, T definition, StateMachine stateMachine) {
        super(planItem, definition);
        this.planItem = planItem;
        this.planItem.setStateMachine(stateMachine);
    }

    /**
     * Returns the id of the instance.
     *
     * @return
     */
    public String getId() {
        return planItem.getId();
    }

    /**
     * Returns the plan item that instantiated this definition
     *
     * @return
     */
    public PlanItem getPlanItem() {
        return planItem;
    }

    public String toString() {
        String prefix = "CasePlanModel";
        if (this.planItem != null) {
            prefix = this.planItem.getName();
        }
        return prefix + " (" + this.getClass().getSimpleName() + " '" + this.getDefinition().getName() + "'). Current state is " + planItem.getState();
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    /**
     * Default Guard implementation for an intended transition on the plan item. Typical implementation inside a Stage to check whether completion is allowed, or in HumanTask to check whether the
     * current user has sufficient roles to e.g. complete a task.
     *
     * @param transition - The transition that the plan item is about to undergo
     * @return
     */
    protected boolean isTransitionAllowed(Transition transition) {
        return true;
    }

    protected void createInstance() {
    }

    protected void completeInstance() {
    }

    protected void terminateInstance() {
    }

    protected void startInstance() {
    }

    protected void suspendInstance() {
    }

    protected void resumeInstance() {
    }

    protected void reactivateInstance() {
    }

    protected void failInstance() {
    }

    protected void dumpMemoryStateToXML(Element planItemXML) {
    }

    protected void retrieveDiscretionaryItems(Collection<DiscretionaryItem> items) {
    }

    /**
     * Indicates whether discretionary items are available for planning (applicable only for Stages and HumanTasks)
     * @return false if it is a milestone or eventlistener
     */
    protected boolean hasDiscretionaryItems() {
        return false;
    }
}
