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

package org.cafienne.cmmn.instance;

import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.cmmn.actorapi.event.CaseAppliedPlatformUpdate;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.*;
import org.cafienne.util.Guid;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
     */
    void planChild(DiscretionaryItem discretionaryItem, String planItemId) {
        // Determine index by iterating sibling plan items (i.e., those that have the same item definition).
        int index = Long.valueOf(this.planItems.stream().filter(item -> item.getItemDefinition().equals(discretionaryItem.getDefinition())).count()).intValue();
        addChild(discretionaryItem.getDefinition(), planItemId, index, true);
    }

    /**
     * Repeat one of our children.
     *
     * @param child The child to be repeated.
     */
    PlanItem<?> repeatChild(PlanItem<?> child) {
        // Generate an id for the repeat item
        String repeatItemId = new Guid().toString();
        ItemDefinition itemDefinition = child.getItemDefinition();
        // Make sure we have a proper next index, by counting the number of existing plan items in this stage with the same definition
        //  An alternative was to do (this.index + 1) but actually it can happen that multiple items are active simultaneously
        //  and in that case, when completing an earlier one of those it may lead to a duplicate index if we apply only +1
        int nextIndex = (int) getPlanItems().stream().map(PlanItem::getItemDefinition).filter(sibling -> sibling.equals(itemDefinition)).count();
        // Create a new plan item
        addDebugInfo(() -> child + ": creating repeat item with index " + nextIndex + " and id " + repeatItemId);
        return addChild(itemDefinition, repeatItemId, nextIndex, true);
    }

    private PlanItem<?> addChild(ItemDefinition itemDefinition, String planItemId, int index, boolean triggerCreateTransition) {
        if (getCaseInstance().recoveryRunning()) {
            return null;
        }
        PlanItemCreated pic = addEvent(new PlanItemCreated(this, itemDefinition, planItemId, index));
        if (triggerCreateTransition && this.getState().isActive()) {
            // Only generate a start transition for the new discretionary item if this stage is active.
            //  Otherwise, the start transition will be generated when this stage becomes active.
            pic.getCreatedPlanItem().create();
        }
        return pic.getCreatedPlanItem();
    }

    /**
     * Creates the child, but without triggering the lifecycle.
     */
    private void instantiateChild(PlanItemDefinition itemDefinition) {
        addChild(itemDefinition, new Guid().toString(), 0, false);
    }

    /**
     * If the Stage is not Active (either because it is in state Available, or Suspended or Fault)
     * new items may have been added from either planning discretionaries or migrating the case definition.
     * This method iterates all items in State.Null and triggers a 'create' transition for them.
     * These may have been created due to case definition migration while stage was in Fault state
     *
     * @param reason The reason why this method is invoked is printed in the debug log event
     */
    private void invokeCreateOnNullItems(String reason) {
        List<PlanItem<?>> nullItems = planItems.stream().filter(item -> item.getState().isNull()).collect(Collectors.toList());
        if (!nullItems.isEmpty()) {
            addDebugInfo(() -> "Stage[" + getName() + "] / " + reason + ": invoking transition 'create' on " + nullItems.size() + " items:");
            nullItems.forEach(item -> addDebugInfo(() -> " - " + item));
        }
        nullItems.forEach(PlanItem::create);
    }

    private boolean hasNullItems() {
        return planItems.stream().anyMatch(item -> item.getState().isNull());
    }

    @Override
    protected void startInstance() {
        // First trigger the 'create' transition on the already planned discretionary items
        if (hasNullItems()) {
            invokeCreateOnNullItems("creating planned discretionary items");
        }

        // Create the default child plan items
        getDefinition().getPlanItems().forEach(this::instantiateChild);

        // Now trigger the 'create' transition on the newly created children
        invokeCreateOnNullItems("creating default items");

        // When a Stage becomes active, and it has no children, it should immediately try to complete.
        if (getPlanItems().isEmpty()) {
            tryCompletion();
        }
    }

    @Override
    protected void suspendInstance() {
        propagateTransition(Transition.ParentSuspend);
    }

    @Override
    protected void resumeInstance() {
        propagateTransition(Transition.ParentResume);
        invokeCreateOnNullItems("resuming stage");
    }

    @Override
    protected void reactivateInstance() {
        super.reactivateInstance();
        invokeCreateOnNullItems("reactivating stage");
    }

    @Override
    protected void terminateInstance() {
        disconnectChildren(true);
    }

    @Override
    protected void completeInstance() {
        disconnectChildren(false);
    }

    protected void childTransitioned(PlanItem<?> child, PlanItemTransitioned event) {
        if (child.getState().isSemiTerminal()) {
            // Check stage completion (only done when the child transitioned into semi terminal state)
            // Stage completion is also only relevant if we are still Active, not when we have already been terminated or completed
            if (this.getState().isSemiTerminal()) {
                addDebugInfo(() -> "---- " + this + " is in state " + getState() + ", hence skipping completion check for event " + event);
            } else {
                addDebugInfo(() -> "*** " + this + ": trying to complete stage because of " + event);
                tryCompletion();
            }
        }
    }

    /**
     * If a child item of this stage has reached semi-terminal state, then it may try to auto complete the surrounding stage.
     */
    void tryCompletion() {
        if (this.isCompletionAllowed(false)) {
            addDebugInfo(() -> "*** " + this + ": triggering stage completion");
            makeTransition(Transition.Complete);
        }
    }

    private boolean isCompletionAllowed(boolean isManualCompletion) {
        /*
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
        // BOTTOM-LINE interpretation: the stage will try to complete each time a child reaches semi-terminal state, or if the transition to complete is manually invoked
        // Here we check both.
        addDebugInfo(() -> {
            String msg = getPlanItems().stream().map(p -> "\n*   - " + p.toDescription()).collect(Collectors.toList()).toString();
            return "*   checking " + planItems.size() + " plan items for completion:" + msg;
        });
        for (PlanItem<?> childItem : planItems) {
            // There shouldn't be any active item.
            if (childItem.getState().isActive()) {
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
     * Determines whether the plan item is contained within this stage or one of its child stages.
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
    public void migrateItemDefinition(ItemDefinition newItemDefinition, T newDefinition, boolean skipLogic) {
        super.migrateItemDefinition(newItemDefinition, newDefinition, skipLogic);
        // Migrate existing children (potentially dropping and removing them)
        new ArrayList<>(planItems).forEach(item -> migrateChild(item, skipLogic));
        if (skipLogic) return;

        // When the Stage is in state Available, it is not yet active, and starting the Stage
        //  will make it active and then the new children will be instantiated automatically.
        // When Stage is Active, or Failed or Suspended or Enabled or Disabled we can create; whether it is started depends on the state
        // When Stage is Terminated or Completed, there is nothing to be done in the remainder. E.g., we cannot re-activate a completed Stage.
        if (this.getState().isAlive()) {
            // Iterate for newly added children and create them.
            newDefinition.getPlanItems().stream().filter(this::doesNotHaveChild).forEach(this::instantiateChild);
            invokeCreateOnNullItems("migrating stage definition");
        }
    }

    private void migrateChild(PlanItem<?> child, boolean skipLogic) {
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
            migrateChild(child, newChildItemDefinition, skipLogic);
        } else {
            dropChild(child);
        }
    }

    private boolean doesNotHaveChild(PlanItemDefinition newChildDefinition) {
        boolean notHasChild = this.getPlanItems().stream().noneMatch(item -> item.getItemDefinition().getId().equals(newChildDefinition.getId()) || item.getName().equals(newChildDefinition.getName()));
        if (notHasChild) {
            addDebugInfo(() -> this + ": migration found a new child definition " + newChildDefinition.getName() + " of type " + newChildDefinition.getType());
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

    private void migrateChild(PlanItem child, ItemDefinition newChildItemDefinition, boolean skipLogic) {
        addDebugInfo(() -> this + ": migrating child " + child + " to a new definition");
        DefinitionElement currentChildDefinition = child.getDefinition();
        PlanItemDefinitionDefinition newChildDefinition = newChildItemDefinition.getPlanItemDefinition();
        if (currentChildDefinition.getClass().isAssignableFrom(newChildDefinition.getClass())) {
            child.migrateItemDefinition(newChildItemDefinition, newChildDefinition, skipLogic);
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
