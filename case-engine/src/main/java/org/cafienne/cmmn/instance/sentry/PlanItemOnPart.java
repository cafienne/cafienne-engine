/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.sentry.PlanItemOnPartDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
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
    boolean doesNotBelongToSiblingStage(PlanItem planItem) {

        if (belongsToSiblingStage(planItem, sentry.getStage())) {
//            System.out.println("\t" + planItem +" is part of a sibling stage of "+ sentry.getStage());
            return false;
        }
        if (belongsToSiblingStage(sentry.getStage(), findStage(planItem))) {
//            System.out.println("\t" + planItem +" is part of a sibling stage of "+ sentry.getStage());
            return false;
        }

        if (sentry.getStage().contains(planItem)) {
//            System.out.println("\tnot a sibling, because " + planItem +" is contained in " + sentry.getStage());
            return true;
        }
        if (planItem.getStage().contains(sentry.getStage())) {
            return true;
        }

        return true;
    }

    private boolean belongsToSiblingStage(PlanItem source, Stage target) {
        if (source == null) {
            return false;
        }
        if (source.getStage() == target) {
            return false;
        }
        ItemDefinition sourceDefinition = source.getItemDefinition();
        ItemDefinition targetStageDefinition = target.getItemDefinition();
        if (sourceDefinition.equals(targetStageDefinition) && source != target) {
            return true;
        }

        return belongsToSiblingStage(source.getStage(), target);
    }

    private Stage findStage(PlanItem target) {
        if (target instanceof Stage) return (Stage) target;
        return target.getStage();
    }



    @Override
    void connectToCase() {
        for (PlanItem planItem : getCaseInstance().getPlanItems()) {
            if (getDefinition().getSourceDefinition().equals(planItem.getItemDefinition())) {
//                System.out.println("\n\nConnecting criterion: " + getSentry().getCriterion());
//                System.out.println("Plan Item Stage: " + planItem.getStage());
//                System.out.println("My stage: " + getSentry().getStage());
//                System.out.println("So we try to connect");
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

    public PlanItem getSource() {
        return source;
    }

    public Transition getTransition() {
        return lastTransition;
    }

    private PlanItem source;

    public void inform(PlanItem planItem, Transition transition) {
        addDebugInfo(() -> planItem + " informs " + sentry.criterion + " about transition " + transition);
        lastTransition = transition;
        source = planItem;
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
