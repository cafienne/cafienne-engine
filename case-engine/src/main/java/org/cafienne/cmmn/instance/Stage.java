/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.cafienne.cmmn.akka.event.PlanItemCreated;
import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.cafienne.cmmn.instance.sentry.ExitCriterion;
import org.cafienne.util.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Stage<T extends StageDefinition> extends PlanFragment<T> {
    private final static Logger logger = LoggerFactory.getLogger(Stage.class);

    private final Collection<PlanItem> planItems = new ArrayList<PlanItem>();
    private final Map<CriterionDefinition, Criterion> sentries = new LinkedHashMap<>();

    // Below are two flags that are required for the checking of stage completion
    private final boolean autoCompletes; // This is the flag set in the definition.
    private boolean isManualCompletion = true; // This is a status keeping track of the cause of the attempt to complete (this info cannot be passed through the statemachine)

    public Stage(PlanItem planItem, T definition) {
        this(planItem, definition, StateMachine.TaskStage);
    }

    protected Stage(PlanItem planItem, T definition, StateMachine stateMachine) {
        super(planItem, definition, stateMachine);
        this.autoCompletes = getDefinition().autoCompletes();
    }

    public EntryCriterion getEntryCriterion(EntryCriterionDefinition definition) {
        Criterion criterion = sentries.get(definition);
        if (criterion == null) {
            criterion = definition.createInstance(this);
            sentries.put(definition, criterion);
        }
        return (EntryCriterion) criterion;
    }

    public ExitCriterion getExitCriterion(ExitCriterionDefinition definition) {
        Criterion criterion = sentries.get(definition);
        if (criterion == null) {
            criterion = definition.createInstance(this);
            sentries.put(definition, criterion);
        }
        return (ExitCriterion) criterion;
    }

    public Collection<PlanItem> getPlanItems() {
        return planItems;
    }

    /**
     * If a child item of this stage has reached semi-terminal state, then it may try to auto complete the surrounding stage.
     *
     * @param child
     * @param transition
     */
    void tryCompletion(PlanItem child, Transition transition) {
        addDebugInfo(() -> "Checking completion for " + this.getClass().getSimpleName() + " '" + this.getPlanItem().getName() + "' because of semi-terminal child '" + child.getName() + "'->" + transition);
        this.isManualCompletion = false; // Now switch this flag. It is set back in
        if (this.isCompletionAllowed()) {
            addDebugInfo(() -> this.getClass().getSimpleName() + " '" + this.getPlanItem().getName() + "' is allowed to complete, source: '" + child.getName() + "'->" + transition);
            getPlanItem().makeTransition(Transition.Complete);
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
        for (PlanItem childItem : planItems) {
            // There shouldn't be any active item.
            if (childItem.getState() == State.Active) {
                addDebugInfo(() -> "Stage '" + getPlanItem().getName() + "' cannot auto complete, because '" + childItem.getName() + "' is still Active");
                return false;
            }
            if (!childItem.getState().isSemiTerminal()) {
                if (autoCompletes || this.isManualCompletion) { // All required items must be semi-terminal; but only when the stage auto completes OR when there is manual completion
                    if (childItem.isRequired()) { // Stage cannot complete if required items are not in semi-terminal state
                        addDebugInfo(() -> "Stage " + getPlanItem().getName() + " cannot complete, because " + childItem.getName() + " is required and has state " + childItem.getState());
                        return false;
                    }
                } else {
                    // Stage cannot complete if not all children are semi-terminal
                    addDebugInfo(() -> "Stage " + getPlanItem().getName() + " cannot complete, because " + childItem.getName() + " has state " + childItem.getState());
                    return false;
                }
            }
        }

        // And, finally, check if there are no discretionary items, but only if we're not completing manually and autoCompletion is false
        if (!autoCompletes && !isManualCompletion) {
            if (hasDiscretionaryItems()) {
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
        planItems.forEach(item -> item.beginLifecycle());

        // Create the child plan items and begin their life-cycle
        getDefinition().getPlanItems().forEach(planItemDefinition -> {

            // Generate an id for the child item
            String childItemId = new Guid().toString();
            // Create a new plan item
            PlanItem childItem = new PlanItem(childItemId, planItemDefinition, planItemDefinition.getPlanItemDefinition(), getCaseInstance(), this, 0);
            // Begin the lifecycle of the child item
            childItem.beginLifecycle();
        });
    }

    @Override
    protected boolean hasDiscretionaryItems() {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null && table.hasItems(this.getPlanItem())) {
            return true;
        }
        for (PlanItem child : getPlanItems()) {
            if (child.getInstance().hasDiscretionaryItems()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void retrieveDiscretionaryItems(Collection<DiscretionaryItem> items) {
        PlanningTableDefinition table = getDefinition().getPlanningTable();
        if (table != null) {
            table.evaluate(this.getPlanItem(), items);
        }
        // Now also retrieve discretionaries for our children
        getPlanItems().forEach(p -> p.getInstance().retrieveDiscretionaryItems(items));
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
        // TODO: distinguish between tasks/stages and eventlisteners/milestones
        propagateTransition(Transition.Exit); // for tasks and substages
        propagateTransition(Transition.ParentTerminate); // for eventlisteners and milestones
    }

    @Override
    protected void completeInstance() {
        // For compatibility in tests. Although it is wrong.
        //  That is to say, the spec says that upon stage completion nothing should happen to items in Available state. But that is confusing. We choose to make them terminated
        propagateTransition(Transition.Exit); // for tasks and substages
        propagateTransition(Transition.ParentTerminate); // for eventlisteners and milestones
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
    protected void dumpMemoryStateToXML(Element stageXML) {
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
        Stage<?> planItemsParent = planItem.getStage();
        if (planItemsParent == null) {
            return false;
        }
        if (planItemsParent == this) {
            return true;
        }
        return contains(planItemsParent.getPlanItem());
    }

    void plan(DiscretionaryItem discretionaryItem, String planItemId) {
        Long index = this.planItems.stream().filter(planItem -> planItem.getName().equals(discretionaryItem.getDefinition().getName())).count();
        PlanItem planItem = new PlanItem(planItemId, discretionaryItem.getDefinition(), discretionaryItem.getDefinition().getPlanItemDefinition(), getCaseInstance(), this, index.intValue());
        if (this.getPlanItem().getState() == State.Active) {
            // Begin the lifecycle only if the state is active. Otherwise, the plan item will be started when the state becomes active.
            planItem.beginLifecycle();
        }
    }

    void recoverPlanItem(PlanItemCreated event) {
        PlanItemDefinition definition = this.getDefinition().getPlanItem(event.planItemName);
        // If definition == null, try to see if it's a discretionaryItem
        if (definition == null) {
            DiscretionaryItemDefinition diDefinition = this.getDefinition().getDiscretionaryItem(event.planItemName);
            if (diDefinition == null) {
                logger.error("MAJOR ERROR: we cannot find a plan item definition named '" + event.planItemName + "' in stage " + this.getId() + ", and therefore cannot recover plan item " + event);
                return;
            }
            PlanItemDefinitionDefinition reference = diDefinition.getPlanItemDefinition();
            new PlanItem(event.planItemId, diDefinition, reference, getCaseInstance(), this, event.index);
        } else {
            PlanItemDefinitionDefinition reference = definition.getPlanItemDefinition();
            new PlanItem(event.planItemId, definition, reference, getCaseInstance(), this, event.index);
        }
    }
}
