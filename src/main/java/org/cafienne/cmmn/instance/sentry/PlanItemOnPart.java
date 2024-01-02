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

package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.sentry.PlanItemOnPartDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.json.ValueMap;
import org.w3c.dom.Element;

import java.util.ArrayList;

public class PlanItemOnPart extends OnPart<PlanItemOnPartDefinition, PlanItemTransitioned, PlanItem<?>> {
    private boolean isActive;
    private Criterion<?> relatedExitCriterion;
    private PlanItemTransitioned lastEvent;

    public PlanItemOnPart(Criterion<?> criterion, PlanItemOnPartDefinition definition) {
        super(criterion, definition);
    }

    @Override
    protected String getSourceType() {
        return getDefinition().getSourceDefinition().getPlanItemDefinition().getType();
    }

    @Override
    Transition getStandardEvent() {
        return getDefinition().getStandardEvent();
    }

    /**
     * Determines whether the two plan item belongs to the stage or any
     * of their ancestors are sibling to each other
     *
     * @param left
     * @param right
     * @return
     */
    private boolean isNotSomewhereSibling(PlanItem<?> left, PlanItem<?> right) {
        if (right == null) {
            return true;
        }

        PlanItem<?> leftOrAnAncestorOfLeft = left;
        while (leftOrAnAncestorOfLeft != null) {
            if (leftOrAnAncestorOfLeft.getItemDefinition().equals(right.getItemDefinition()) && leftOrAnAncestorOfLeft != right) {
                // If the definitions match and the instances mis-match, then we found somewhere a sibling
                return false;
            }
            leftOrAnAncestorOfLeft = leftOrAnAncestorOfLeft.getStage();
        }
        return isNotSomewhereSibling(left, right.getStage());
    }

    @Override
    void connectToCase() {
        // Try to connect with all plan items in the case
        for (PlanItem<?> item : new ArrayList<>(getCaseInstance().getPlanItems())) {
            establishPotentialConnection(item);
        }
    }

    @Override
    protected void establishPotentialConnection(PlanItem<?> potentialNewSource) {
        if (connectedItems.contains(potentialNewSource)) {
            // Avoid repeated additions
            return;
        }

        // Only connect if the plan item has the same definition as our source definition.
        if (!getDefinition().getSourceDefinition().equals(potentialNewSource.getItemDefinition())) {
            return;
        }

        // Only connect plan items that are in "our" hierarchy
        if (isNotSomewhereSibling(potentialNewSource, getCriterion().getTarget())) {
            addDebugInfo(() -> "Connecting " + potentialNewSource + " to " + criterion);
            connectedItems.add(potentialNewSource);
            potentialNewSource.connectOnPart(this);
            if (getDefinition().getRelatedExitCriterion() != null) {
                relatedExitCriterion = getCaseInstance().getSentryNetwork().findRelatedExitCriterion(potentialNewSource, getDefinition().getRelatedExitCriterion());
            }
        } else {
            addDebugInfo(() -> "Not connecting plan item " + potentialNewSource + " to " + criterion + " because it belongs to a sibling stage");
        }
    }

    @Override
    protected void removeConnection(PlanItem<?> planItem) {
        connectedItems.remove(planItem);
    }

    @Override
    public void releaseFromCase() {
        connectedItems.forEach(planItem -> planItem.releaseOnPart(this));
    }

    public void inform(PlanItem<?> item, PlanItemTransitioned event) {
        addDebugInfo(() -> item + " informs " + criterion + " about transition " + event.getTransition());
        lastEvent = event;
        isActive = getStandardEvent().equals(event.getTransition());
        if (isActive) {
            if (relatedExitCriterion != null) { // The exitCriterion must also be active
                if (relatedExitCriterion.isActive()) {
                    criterion.activate(this);
                } else {
                    addDebugInfo(() -> criterion + ": onPart '" + getSourceName() + "=>" + event.getTransition() + "' is not activated, because related exit criterion is not active", this.criterion.toJson());
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
    ValueMap toJson() {
        return new ValueMap("planitem", getSourceName(),
                "active", isActive,
                "awaiting-transition", getStandardEvent(),
                "last-found-transition", "" + lastEvent
        );
    }

    @Override
    void dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element onPartXML = parentElement.getOwnerDocument().createElement("onPart");
        parentElement.appendChild(onPartXML);
        onPartXML.setAttribute("active", "" + isActive);
        onPartXML.setAttribute("source", getSourceName() + "." + getStandardEvent());
        onPartXML.setAttribute("last", "" + lastEvent);

        if (showConnectedPlanItems) {
            for (PlanItem<?> planItem : connectedItems) {
                String lastTransition = planItem.getName() + "." + planItem.getLastTransition();
                Element planItemXML = parentElement.getOwnerDocument().createElement("planitem");
                planItemXML.setAttribute("last", lastTransition);
                planItemXML.setAttribute("id", planItem.getId());
                planItemXML.setAttribute("stage", planItem.getStage().getId());
                onPartXML.appendChild(planItemXML);
            }
        }
    }
}
