/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.PlanItemOnPartDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class PlanItemOnPart extends OnPart<PlanItemOnPartDefinition, PlanItem<?>> {
    private final Object standardEvent;
    private final String sourceName;
    private boolean isActive;
    private Transition lastTransition;
    private ExitCriterion relatedExitCriterion;

    public PlanItemOnPart(Sentry sentry, PlanItemOnPartDefinition definition) {
        super(sentry, definition);
        this.standardEvent = definition.getStandardEvent();
        this.sourceName = definition.getSourceDefinition().getName();
    }

    /**
     * Determines whether the plan item belongs to the stage of this sentry or
     * to one of it's parents or descendants. If the plan item belongs to a
     * sibling stage, then it should not be connected to this sentry
     *
     * @param planItem
     * @return
     */
    private boolean doesNotBelongToSiblingStage(PlanItem planItem) {
        if (sentry.getStage().contains(planItem)) {
            return true;
        }
        if (planItem.getStage().contains(sentry.getStage())) {
            return true;
        }
        return false;
    }

    @Override
    void connectToCase() {
        for (PlanItem planItem : getCaseInstance().getPlanItems()) {
            if (getDefinition().getSourceDefinition().equals(planItem.getItemDefinition())) {
                connect(planItem);
            }
        }
    }

    void connect(PlanItem planItem) {
        if (doesNotBelongToSiblingStage(planItem)) {
            addDebugInfo(() -> "Connecting " + planItem + " to " + sentry.criterion);
            connectedItems.add(planItem);
            planItem.connectOnPart(this);
            if (getDefinition().getRelatedExitCriterion() != null) {
                relatedExitCriterion = planItem.getStage().getExitCriterion(getDefinition().getRelatedExitCriterion());
            }
        } else {
            addDebugInfo(() -> "Not connecting plan item " + planItem + " to " + sentry.criterion + " because it belongs to a sibling stage");
        }
    }

    public void inform(PlanItem planItem, Transition transition) {
        addDebugInfo(() -> "Plan item " + planItem + " informs " + sentry.criterion + " about transition " + transition);
        lastTransition = transition;
        isActive = standardEvent.equals(lastTransition);
        if (isActive) {
            if (relatedExitCriterion != null) { // The exitCriterion must also be active
                if (relatedExitCriterion.isActive()) {
                    sentry.activate(this);
                } else {
                    addDebugInfo(() -> sentry.criterion + ": onPart '" + sourceName + "=>" + lastTransition + "' is not activated, because related exit criterion is not active", this.sentry);
                }
            } else {
                // Bingo, we have a hit
                sentry.activate(this);
            }
        } else {
            sentry.deactivate(this);
        }
    }

    @Override
    public String toString() {
        return sentry.toString() + ".on." + sourceName + "" + standardEvent;
    }

    @Override
    ValueMap toJson() {
        return new ValueMap("planitem", sourceName,
            "active", isActive,
            "awaiting-transition", standardEvent,
            "last-found-transition", lastTransition
        );
    }

    @Override
    Element dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element onPartXML = parentElement.getOwnerDocument().createElement("onPart");
        parentElement.appendChild(onPartXML);
        onPartXML.setAttribute("active", "" + isActive);
        onPartXML.setAttribute("source", sourceName + "." + standardEvent);
        onPartXML.setAttribute("last", sourceName + "." + lastTransition);

        if (showConnectedPlanItems) {
            for (PlanItem planItem : connectedItems) {
                String lastTransition = planItem.getName() + "." + planItem.getLastTransition();
                Element planItemXML = parentElement.getOwnerDocument().createElement("planitem");
                planItemXML.setAttribute("last", lastTransition);
                planItemXML.setAttribute("id", planItem.getId());
                planItemXML.setAttribute("stage", planItem.getStage().getId());
                onPartXML.appendChild(planItemXML);
            }
        }

        return onPartXML;
    }
}
