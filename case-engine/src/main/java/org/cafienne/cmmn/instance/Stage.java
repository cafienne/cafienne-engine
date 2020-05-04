/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.plan.PlanItemCreated;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.PlanningTableDefinition;
import org.cafienne.cmmn.definition.StageDefinition;
import org.cafienne.util.Guid;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class Stage<T extends StageDefinition> extends PlanFragment<T> {
    private final Collection<PlanItem> planItems = new ArrayList<PlanItem>();

    // Below are two flags that are required for the checking of stage completion
    private final boolean autoCompletes; // This is the flag set in the definition.
    private boolean isManualCompletion = true; // This is a status keeping track of the cause of the attempt to complete (this info cannot be passed through the statemachine)

    public Stage(String id, int index, ItemDefinition itemDefinition, T definition, Stage parent, Case caseInstance) {
        this(id, index, itemDefinition, definition, parent, caseInstance, StateMachine.TaskStage);
    }

    protected Stage(String id, int index, ItemDefinition itemDefinition, T definition, Stage parent, Case caseInstance, StateMachine stateMachine) {
        super(id, itemDefinition, definition, caseInstance, parent, index, stateMachine);
        this.autoCompletes = definition.autoCompletes();
    }

    void register(PlanItem child) {
        planItems.add(child);
    }

    public Collection<PlanItem> getPlanItems() {
        return planItems;
    }

    /**
     * If a child item of this stage has reached semi-terminal state, then it may try to auto complete the surrounding stage.
     *
     * @param event
     */
    void tryCompletion(PlanItemTransitioned event) {
        // Stage completion check is only done for transitions into semi terminal state, so if not we can immediately return.
        if (! event.getCurrentState().isSemiTerminal()) {
            return;
        }

        // Stage completion is also only relevant if we are still Active, not when we have already been terminated or completed
        if (this.getState().isSemiTerminal()) {
            addDebugInfo(() -> "---- " + this + " is in state " + getState() + ", hence skipping completion check for event " + event);
            return;
        }
        addDebugInfo(() -> "*** " + this + ": checking completion because of " + event);
        this.isManualCompletion = false; // Now switch this flag. It is set back in
        if (this.isCompletionAllowed()) {
            addDebugInfo(() -> "*** " + this + ": triggering stage completion");
            makeTransition(Transition.Complete);
        }
        this.isManualCompletion = true; // Now switch the flag back. Is only required if isCompletionAllowed returns false.
    }

    private boolean isCompletionAllowed() {
        /**
         * Quote from the spec chapter 7.6.1 (cmmn 1.1: chapter 8.6.1):
         *
         * When autocomplete is true:
         * There are no Active children, AND all required (requiredRule evaluates to TRUE) children are in {Disabled, Completed, Terminated, Failed}.
         * When autocomplete is false:
         * There are no Active children AND
         *  (all children are in {Disabled, Completed, Terminated, Failed} AND there are no DiscretionaryItems)
         *   OR (Manual Completion AND all required (requiredRule evaluates to TRUE) children are in {Disabled, Completed, Terminated, Failed}).
         *
         * In other words, a Stage instance SHOULD complete if a user has no option to do further planning or work with the Stage instance.
         */
        // BOTTOMLINE interpretation: the stage will try to complete each time a child reaches semiterminal state, or if the transition to complete is manually invoked
        // Here we check both.
        addDebugInfo(() -> {
            String msg = getPlanItems().stream().map(p -> "\n*   - " + p.toDescription()).collect(Collectors.toList()).toString();
            return "*   checking " + planItems.size() +" plan items for completion:" + msg;
        });
        for (PlanItem childItem : planItems) {
            // There shouldn't be any active item.
            if (childItem.getState() == State.Active) {
                addDebugInfo(() -> "*** " + this + " cannot auto complete, because '" + childItem.toDescription() + "' is still Active");
                return false;
            }
            if (!childItem.getState().isSemiTerminal()) {
                if (autoCompletes || this.isManualCompletion) { // All required items must be semi-terminal; but only when the stage auto completes OR when there is manual completion
                    if (childItem.isRequired()) { // Stage cannot complete if required items are not in semi-terminal state
                        addDebugInfo(() -> "*** " + this + " cannot auto complete, because " + childItem.toDescription() + " is required and has state " + childItem.getState());
                        return false;
                    }
                } else {
                    // Stage cannot complete if not all children are semi-terminal
                    addDebugInfo(() -> "*** " + this + " cannot auto complete, because " + childItem.toDescription() + " has state " + childItem.getState());
                    return false;
                }
            }
        }

        // And, finally, check if there are no discretionary items, but only if we're not completing manually and autoCompletion is false
        if (!autoCompletes && !isManualCompletion) {
            if (hasDiscretionaryItems()) {
                addDebugInfo(() -> "*** " + this + " cannot auto complete, because there are still discretionary items");
                return false;
            }
        }

        // In the end, we're all set to complete
        return true;
    }

    @Override
    protected boolean isTransitionAllowed(Transition transition) {
        if (transition == Transition.Complete) {
            if (!this.isManualCompletion) {
                this.isManualCompletion = true; // Switch back the flag; completion allowed is already checked, so no need to check it again.
                return true;
            } else {
                return isCompletionAllowed();
            }
        } else {
            return true;
        }
    }

    @Override
    protected void startInstance() {
        // First start the discretionary items that have already been planned.
        planItems.forEach(item -> item.makeTransition(Transition.Create));

        // Create the child plan items and begin their life-cycle
        getDefinition().getPlanItems().forEach(itemDefinition -> {
            int index = Long.valueOf(this.planItems.stream().filter(planItem -> planItem.getName().equals(itemDefinition.getDefinition().getName())).count()).intValue();

            // Generate an id for the child item
            String childItemId = new Guid().toString();
            PlanItemCreated pic = new PlanItemCreated(this, itemDefinition, childItemId, index);
            getCaseInstance().addEvent(pic);
            pic.getCreatedPlanItem().makeTransition(Transition.Create);
        });
    }

    @Override
    protected boolean hasDiscretionaryItems() {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null && table.hasItems(this)) {
            return true;
        }
        for (PlanItem child : getPlanItems()) {
            if (child.hasDiscretionaryItems()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void retrieveDiscretionaryItems(Collection<DiscretionaryItem> items) {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null) {
            addDebugInfo(() -> "Iterating planning table items in " + this);
            table.evaluate(this, items);
        }
        // Now also retrieve discretionaries for our children
        getPlanItems().forEach(p -> p.retrieveDiscretionaryItems(items));
    }

    @Override
    protected void suspendInstance() {
        propagateTransition(Transition.ParentSuspend);
    }

    @Override
    protected void resumeInstance() {
        propagateTransition(Transition.ParentResume);
    }

    @Override
    protected void terminateInstance() {
        disconnectChildren(true);
    }

    @Override
    protected void completeInstance() {
        disconnectChildren(false);
    }

    private void disconnectChildren(boolean makeTerminationTransition) {
        for (PlanItem child : planItems) {
            if (makeTerminationTransition ) {
                child.makeTransition(child.getTerminationTransition());
            }
            child.getEntryCriteria().release();
            child.getExitCriteria().release();
        }
    }

    /**
     * Invoked if the stage arrives in a certain state. Based on the transition we will get particular transitions on the children.
     *
     * @param transition
     */
    private void propagateTransition(Transition transition) {
        for (PlanItem child : planItems) {
            child.makeTransition(transition);
        }
    }

    @Override
    protected void dumpImplementationToXML(Element stageXML) {
        super.dumpImplementationToXML(stageXML);
        for (PlanItem child : planItems) {
            child.dumpMemoryStateToXML(stageXML);
        }

        if (this.getDefinition().getPlanningTable() != null) {
            stageXML.appendChild(stageXML.getOwnerDocument().createComment(" Planning table "));
            this.getDefinition().getPlanningTable().dumpMemoryStateToXML(stageXML, this);
        }
    }

    /**
     * Determines whether the plan item is contained within this stage or one of it's child stages.
     *
     * @param planItem
     * @return
     */
    public boolean contains(PlanItem planItem) {
        if (planItem == null) {
            return false;
        }
        Stage planItemsParent = planItem.getStage();
        if (planItemsParent == null) {
            return false;
        }
        if (planItemsParent == this) {
            return true;
        }
        return contains(planItemsParent);
    }

    void plan(DiscretionaryItem discretionaryItem, String planItemId) {
        int index = Long.valueOf(this.planItems.stream().filter(planItem -> planItem.getName().equals(discretionaryItem.getDefinition().getName())).count()).intValue();

        PlanItemCreated pic = new PlanItemCreated(this, discretionaryItem.getDefinition(), planItemId, index);
        getCaseInstance().addEvent(pic);
        if (this.getState() == State.Active) {
            // Only generate a start transition for the new discretionary item if this stage is active.
            //  Otherwise the start transition will be generated when this stage becomes active.
            pic.getCreatedPlanItem().makeTransition(Transition.Create);
        }
    }
}
