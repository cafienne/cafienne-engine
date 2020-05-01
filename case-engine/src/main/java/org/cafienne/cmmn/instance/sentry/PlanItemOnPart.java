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
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.w3c.dom.Element;

import java.util.stream.Collectors;

public class PlanItemOnPart extends OnPart<PlanItemOnPartDefinition, PlanItem<?>> {
    private final Transition standardEvent;
    private final String sourceName;
    private boolean isActive;
    private ExitCriterion relatedExitCriterion;
    private StandardEvent lastEvent;

    public PlanItemOnPart(Criterion criterion, PlanItemOnPartDefinition definition) {
        super(criterion, definition);
        this.standardEvent = definition.getStandardEvent();
        this.sourceName = definition.getSourceDefinition().getName();
    }

    /**
     * Determines whether the plan item belongs to the stage of this criterion or
     * to one of it's parents or descendants. If the plan item belongs to a
     * sibling stage, then it should not be connected to this criterion
     *
     * @param planItem
     * @return
     */
    boolean doesNotBelongToSiblingStage(PlanItem planItem) {

        if (belongsToSiblingStage(planItem, criterion.getStage())) {
//            System.out.println("\t" + planItem +" is part of a sibling stage of "+ getStage());
            return false;
        }
        if (belongsToSiblingStage(criterion.getStage(), findStage(planItem))) {
//            System.out.println("\t" + planItem +" is part of a sibling stage of "+ getStage());
            return false;
        }

        if (criterion.getStage().contains(planItem)) {
//            System.out.println("\tnot a sibling, because " + planItem +" is contained in " + getStage());
            return true;
        }
        if (planItem.getStage().contains(criterion.getStage())) {
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
        // Try to connect with all plan items in the case
        for (PlanItem item : getCaseInstance().getPlanItems()) {
            criterion.establishPotentialConnection(item);
        }
    }

    void connect(PlanItem planItem) {
        if (connectedItems.contains(planItem)) {
            // Avoid repeated additions
            return;
        }
        if (doesNotBelongToSiblingStage(planItem)) {
            addDebugInfo(() -> "Connecting " + planItem + " to " + criterion);
            connectedItems.add(planItem);
            planItem.connectOnPart(this);
            if (getDefinition().getRelatedExitCriterion() != null) {
                relatedExitCriterion = planItem.getStage().getExitCriterion(getDefinition().getRelatedExitCriterion());
            }
        } else {
            addDebugInfo(() -> "Not connecting plan item " + planItem + " to " + criterion + " because it belongs to a sibling stage");
        }
    }

    public PlanItem getSource() {
        return source;
    }
    private PlanItem source;

    public void inform(PlanItem item, StandardEvent event) {
        addDebugInfo(() -> "Case file item " + item.getPath() + " informs " + criterion + " about transition " + event.getTransition() + ".");
        lastEvent = event;
        source = item;
        isActive = standardEvent.equals(event.getTransition());
        if (isActive) {
            if (relatedExitCriterion != null) { // The exitCriterion must also be active
                if (relatedExitCriterion.isActive()) {
                    criterion.activate(this);
                } else {
                    addDebugInfo(() -> criterion + ": onPart '" + sourceName + "=>" + event.getTransition() + "' is not activated, because related exit criterion is not active", this.criterion);
                }
            } else {
                // Bingo, we have a hit
                criterion.activate(this);
            }
        } else {
            criterion.deactivate(this);
        }
    }

    @Override
    public String toString() {
        String printedItems = connectedItems.isEmpty() ? "No items '" + sourceName+"' connected" : connectedItems.stream().map(item -> item.getPath()).collect(Collectors.joining(","));
        return standardEvent +" of " + printedItems;
    }

    @Override
    ValueMap toJson() {
        return new ValueMap("planitem", sourceName,
            "active", isActive,
            "awaiting-transition", standardEvent,
            "last-found-transition", "" + lastEvent
        );
    }

    @Override
    Element dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element onPartXML = parentElement.getOwnerDocument().createElement("onPart");
        parentElement.appendChild(onPartXML);
        onPartXML.setAttribute("active", "" + isActive);
        onPartXML.setAttribute("source", sourceName + "." + standardEvent);
        onPartXML.setAttribute("last", "" + lastEvent);

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
