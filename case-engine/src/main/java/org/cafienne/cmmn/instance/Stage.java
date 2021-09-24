/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.cmmn.actorapi.event.CaseAppliedPlatformUpdate;
import org.cafienne.cmmn.actorapi.event.migration.PlanItemDropped;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.*;
import org.cafienne.util.Guid;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class Stage<T extends StageDefinition> extends TaskStage<T> {
    private final Collection<PlanItem<?>> planItems = new ArrayList<>();

    public Stage(String id, int index, ItemDefinition itemDefinition, T definition, Stage<?> parent, Case caseInstance) {
        this(id, index, itemDefinition, definition, parent, caseInstance, StateMachine.TaskStage);
    }

    protected Stage(String id, int index, ItemDefinition itemDefinition, T definition, Stage<?> parent, Case caseInstance, StateMachine stateMachine) {
        super(id, index, itemDefinition, definition, caseInstance, parent, stateMachine);
    }

    void register(PlanItem<?> child) {
        if (getCaseInstance().recoveryRunning() && child.getIndex() > 0) {
            planItems.stream().filter(p -> p.getDefinition().equals(child.getDefinition()) && p.getIndex() + 1 == child.getIndex()).forEach(leftSibling -> {
//                System.out.println("!!!Releasing already repeated plan item " + leftSibling);
                // Recovering repeated plan items should no longer inform the sentry network
                leftSibling.getEntryCriteria().release();
            });
        }
        planItems.add(child);
    }

    public Collection<PlanItem<?>> getPlanItems() {
        return planItems;
    }

    /**
     * Adds an item to the stage, by adding a PlanItemCreated event for it.
     * Also starts the lifecycle of the new plan item if the Stage is currently active.
     * @param discretionaryItem
     * @param planItemId
     */
    void planChild(DiscretionaryItem discretionaryItem, String planItemId) {
        // Determine index by iterating sibling plan items (i.e., those that have the same item definition).
        int index = Long.valueOf(this.planItems.stream().filter(item -> item.getItemDefinition().equals(discretionaryItem.getDefinition())).count()).intValue();
        addChild(discretionaryItem.getDefinition(), planItemId, index);
    }

    PlanItem<?> addChild(ItemDefinition itemDefinition, String planItemId, int index) {
        if (getCaseInstance().recoveryRunning()) {
            return null;
        }
        PlanItemCreated pic = addEvent(new PlanItemCreated(this, itemDefinition, planItemId, index));
        if (this.getState() == State.Active) {
            // Only generate a start transition for the new discretionary item if this stage is active.
            //  Otherwise the start transition will be generated when this stage becomes active.
            pic.getCreatedPlanItem().makeTransition(Transition.Create);
        }
        return pic.getCreatedPlanItem();
    }

    private void createChild(PlanItemDefinition itemDefinition) {
        addChild(itemDefinition, new Guid().toString(), 0);
    }

    /**
     * If the Stage is not Active (either because it is in state Available, or Suspended or Fault)
     * new items may have been added from either planning discretionaries or migrating the case definition.
     * This method iterates all items in State.Null and triggers a create transition for them.
     *  These may have been created due to case definition migration while stage was in Fault state
     */
    private void createNullItems() {
        addDebugInfo(() -> "Instantiating life cycle for null items. Total plan items in Stage[" + getName()+ " is " + planItems.size());
        addDebugInfo(() -> {
            StringBuilder sb = new StringBuilder("Items:\n");
            planItems.forEach(item -> sb.append("Item["+item.getName()+"."+item.getIndex() +"] in state "+ item.getState()));
            return sb.toString();
        });
        planItems.stream().filter(item -> item.getState().isNull()).forEach(item -> item.makeTransition(Transition.Create));
    }

    @Override
    protected void startInstance() {
        // First start the discretionary items that have already been planned.
        createNullItems();

        // Create the child plan items and begin their life-cycle
        getDefinition().getPlanItems().forEach(this::createChild);
    }

    @Override
    protected void suspendInstance() {
        propagateTransition(Transition.ParentSuspend);
    }

    @Override
    protected void resumeInstance() {
        propagateTransition(Transition.ParentResume);
        createNullItems();
    }

    @Override
    protected void reactivateInstance() {
        super.reactivateInstance();
        createNullItems();
    }

    @Override
    protected void terminateInstance() {
        disconnectChildren(true);
    }

    @Override
    protected void completeInstance() {
        disconnectChildren(false);
    }

    /**
     * If a child item of this stage has reached semi-terminal state, then it may try to auto complete the surrounding stage.
     *
     * @param event
     */
    void tryCompletion(PlanItemTransitioned event) {
        // Stage completion is also only relevant if we are still Active, not when we have already been terminated or completed
        if (this.getState().isSemiTerminal()) {
            addDebugInfo(() -> "---- " + this + " is in state " + getState() + ", hence skipping completion check for event " + event);
            return;
        }
        addDebugInfo(() -> "*** " + this + ": checking completion because of " + event);
        if (this.isCompletionAllowed(false)) {
            addDebugInfo(() -> "*** " + this + ": triggering stage completion");
            makeTransition(Transition.Complete);
        }
    }

    private boolean isCompletionAllowed(boolean isManualCompletion) {
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
            return "*   checking " + planItems.size() + " plan items for completion:" + msg;
        });
        for (PlanItem<?> childItem : planItems) {
            // There shouldn't be any active item.
            if (childItem.getState() == State.Active) {
                addDebugInfo(() -> "*** " + this + " cannot auto complete, because '" + childItem.toDescription() + "' is still Active");
                return false;
            }
            if (!childItem.getState().isSemiTerminal()) {
                if (getDefinition().autoCompletes() || isManualCompletion) { // All required items must be semi-terminal; but only when the stage auto completes OR when there is manual completion
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
        if (!getDefinition().autoCompletes() && !isManualCompletion) {
            if (hasDiscretionaryItems()) {
                addDebugInfo(() -> "*** " + this + " cannot auto complete, because there are still discretionary items");
                return false;
            }
        }

        // In the end, we're all set to complete
        return true;
    }

    @Override
    public void validateTransition(Transition transition) {
        super.validateTransition(transition);
        if (transition == Transition.Complete) {
            if (!isCompletionAllowed(true)) {
                throw new InvalidCommandException("Cannot complete the stage as there are active items remaining");
            }
        }
    }

    @Override
    protected boolean hasDiscretionaryItems() {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null && table.hasItems(this)) {
            return true;
        }
        for (PlanItem<?> child : getPlanItems()) {
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

    private void disconnectChildren(boolean makeTerminationTransition) {
        for (PlanItem<?> child : planItems) {
            if (makeTerminationTransition) {
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
        for (PlanItem<?> child : planItems) {
            child.makeTransition(transition);
        }
    }

    @Override
    protected void dumpImplementationToXML(Element stageXML) {
        super.dumpImplementationToXML(stageXML);
        for (PlanItem<?> child : planItems) {
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
    public boolean contains(PlanItem<?> planItem) {
        if (planItem == null) {
            return false;
        }
        Stage<?> planItemsParent = planItem.getStage();
        if (planItemsParent == null) {
            return false;
        }
        if (planItemsParent == this) {
            return true;
        }
        return contains(planItemsParent);
    }

    @Override
    public void updateState(CaseAppliedPlatformUpdate event) {
        planItems.forEach(item -> item.updateState(event));
    }

    @Override
    public void migrateItemDefinition(ItemDefinition newItemDefinition, T newDefinition) {
        super.migrateItemDefinition(newItemDefinition, newDefinition);
        // Migrate existing children (potentially dropping and removing them)
        new ArrayList<>(planItems).forEach(this::migrateChild);

        // When the Stage is in state Available, it is not yet active, and starting the Stage
        //  will make it active and then the new children will be instantiated automatically.
        // When Stage is Active, or Failed or Suspended or Enabled or Disabled we can create; whether it is started depends on the state
        // When Stage is Terminated or Completed, there is nothing to be done in the remainder. E.g., we cannot re-activate a completed Stage.
        if (this.getState().isAlive()) {
            // Iterate for newly added children and create them.
            newDefinition.getPlanItems().stream().filter(this::doesNotHaveChild).forEach(this::createChild);
        }
    }

    private void migrateChild(PlanItem<?> child) {
        String childDefinitionId = child.getItemDefinition().getId();
        String childName = child.getItemDefinition().getName();

        // Figure out if we can find a new definition for the child
        ItemDefinition newChildItemDefinition = getDefinition().getPlanItem(childDefinitionId);
        if (newChildItemDefinition == null) {
            // Perhaps get by name?
            newChildItemDefinition = getDefinition().getPlanItem(childName);
            if (newChildItemDefinition == null) {
                // Perhaps it is a discretionary item?
                newChildItemDefinition = getDefinition().getDiscretionaryItem(childDefinitionId);
                if (newChildItemDefinition == null) {
                    newChildItemDefinition = getDefinition().getDiscretionaryItem(childName);
                }
            }
        }

        // If we found a new definition, let's migrate it; otherwise tell the child to get lost.
        if (newChildItemDefinition != null) {
            migrateChild(child, newChildItemDefinition);
        } else {
            dropChild(child);
        }
    }

    private boolean doesNotHaveChild(PlanItemDefinition newChildDefinition) {
        boolean notHasChild = this.getPlanItems().stream().noneMatch(item -> item.getItemDefinition().getId().equals(newChildDefinition.getId()) || item.getName().equals(newChildDefinition.getName()));
        if (notHasChild) {
            MigDevConsole("Stage does not have a child with definition " + newChildDefinition.getName() +" of type " + newChildDefinition.getType() +", hence creating a new one");
        }
        return notHasChild;
    }

    private void dropChild(PlanItem<?> child) {
        // No need to drop children when recovery is running, as the below generated PlanItemDropped event will be recovered soon enough...
        if (getCaseInstance().recoveryRunning()) {
            return;
        }
        child.lostDefinition();
    }

    @Override
    protected void lostDefinition() {
        // First drop our children
        new ArrayList<>(planItems).forEach(PlanItem::lostDefinition);
        // Then generate our own event
        super.lostDefinition();
    }

    private void migrateChild(PlanItem child, ItemDefinition newChildItemDefinition) {
        MigDevConsole("Migrating Stage child " + child +" to a new definition");
        DefinitionElement currentChildDefinition = child.getDefinition();
        PlanItemDefinitionDefinition newChildDefinition = newChildItemDefinition.getPlanItemDefinition();
        if (currentChildDefinition.getClass().isAssignableFrom(newChildDefinition.getClass())) {
            child.migrateItemDefinition(newChildItemDefinition, newChildDefinition);
        } else {
            // Apparently what was once a Task is now a Stage or so?
            // Scenario not yet supported...
            addDebugInfo(() -> "Not possible to migrate from " + currentChildDefinition.getType() + " to " + newChildDefinition.getType());
        }
    }

    void removeDroppedPlanItem(PlanItem<?> item) {
        planItems.remove(item);
    }
}
