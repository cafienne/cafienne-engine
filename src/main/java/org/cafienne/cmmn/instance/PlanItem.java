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
import org.cafienne.cmmn.actorapi.event.migration.PlanItemMigrated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemEvent;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.actorapi.event.plan.RepetitionRuleEvaluated;
import org.cafienne.cmmn.actorapi.event.plan.RequiredRuleEvaluated;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.PlanItemDefinitionDefinition;
import org.cafienne.cmmn.instance.sentry.PlanItemOnPart;
import org.cafienne.cmmn.instance.sentry.TransitionGenerator;
import org.cafienne.util.Guid;
import org.w3c.dom.Element;

import java.util.Collection;

public abstract class PlanItem<T extends PlanItemDefinitionDefinition> extends CMMNElement<T> implements TransitionGenerator<PlanItemTransitioned> {
    /**
     * Unique identifier of the plan item, typically a guid
     */
    private final String id;
    /**
     * If the plan item repeats, each item has 1 higher index (0, 1, 2, ...)
     */
    private final int index;
    /**
     * Stage to which this plan item belongs (null for CasePlan)
     */
    private final Stage<?> stage;
    /**
     * The actual plan item definition (or discretionary item definition, or caseplan definition).
     */
    private ItemDefinition itemDefinition;
    /**
     * Previous item definition for reference and comparison if the itemDefinition is migrated
     */
    private ItemDefinition previousItemDefinition;
    /**
     * State machine that executes behavior
     */
    private final StateMachine stateMachine;

    /**
     * Our entry and exit criteria (i.e., to plan items and case file items of interest to us)
     */
    private final PlanItemEntry entryCriteria;
    private final PlanItemExit exitCriteria;
    /**
     * Whether we repeat or not
     */
    private boolean repetitionRuleOutcome;
    /**
     * Whether we are required
     */
    private boolean requiredRuleOutcome;
    /**
     * Current state machine state and last transition and history state.
     */
    private final PlanItemTransitionPublisher transitionPublisher = new PlanItemTransitionPublisher(this);
    private Transition lastTransition = Transition.None;
    private State state = State.Null;
    private State historyState = State.Null;
    /**
     * Number of PlanItem events we have generated
     */
    private int planItemEventCounter = 0; // Akka event sequence number

    protected PlanItem(String id, int index, ItemDefinition itemDefinition, T definition, Stage<?> parent, StateMachine stateMachine) {
        this(id, index, itemDefinition, definition, parent.getCaseInstance(), parent, stateMachine);
    }

    /**
     * Constructor for CasePlan (and Stages also use it)
     *
     * @param id
     * @param index
     * @param itemDefinition
     * @param definition
     * @param caseInstance
     * @param parent
     * @param stateMachine
     */
    protected PlanItem(String id, int index, ItemDefinition itemDefinition, T definition, Case caseInstance, Stage<?> parent, StateMachine stateMachine) {
        super(caseInstance, definition);
        this.id = id;
        this.itemDefinition = itemDefinition;
        this.stage = parent;
        this.index = index;
        this.stateMachine = stateMachine;
        this.entryCriteria = new PlanItemEntry(this);
        this.exitCriteria = new PlanItemExit(this);

        addDebugInfo(() -> "Constructing plan item " + this + " with id " + id + (getStage() == null ? " in case" : " in " + getStage()));

        // Register at case level
        getCaseInstance().registerPlanItem(this);

        // Now connect to the rest of the network of plan items and sentries
        if (getStage() != null) {
            // Register with parent stage
            getStage().register(this);
        }

        // Link ourselves to any existing sentries in the case
        getCaseInstance().getSentryNetwork().connect(this);
    }

    @Override
    public PlanItemTransitionPublisher getPublisher() {
        return transitionPublisher;
    }

    public void connectOnPart(PlanItemOnPart onPart) {
        getPublisher().connectOnPart(onPart);
    }

    public void releaseOnPart(PlanItemOnPart onPart) {
        getPublisher().releaseOnPart(onPart);
    }

    /**
     * Repeats the plan item upon it's Completion or Termination, and only if there are no entry criteria.
     * Additionally checks that the containing stage is still active.
     */
    void repeat() {
        addDebugInfo(() -> this + ": initiating repeat logic for next item. First releasing entry criteria");
        // Repeat the plan item when it is in Completed or Terminated state - Or if it has entry criteria being met
        if (getStage().getState() != State.Active) {
            // The stage that contains us is no longer active. So we will not prepare any repeat item.
            // This code is typically invoked if the the stage terminates or completes, causing the repeating element to terminate.
            addDebugInfo(() -> this + ": not repeating because stage is not active");
            return;
        }

        // Re-evaluate the repetition rule, and if the outcome is true, the start the repeat item (but only if it is not a discretionary).
        addDebugInfo(() -> this + ": evaluating RepetitionRule from within repeat()");
        evaluateRepetitionRule(false);
        if (repeats()) {
            if (getItemDefinition().isDiscretionary()) {
                // Means we are discretionary, and adding to the plan must be done manually
                return;
            }
            // Generate an id for the repeat item
            String repeatItemId = new Guid().toString();
            // Create a new plan item
            addDebugInfo(() -> this + ": creating repeat item " + (index + 1) + " with id " + repeatItemId);
            PlanItem<?> repeatedItem = stage.addChild(getItemDefinition(), repeatItemId, index + 1);
            // Also let it Start immediately (or Occur, or Enable)
            repeatedItem.makeTransition(getEntryTransition());
        }
    }

    /**
     * Tries to set a lock for the next transition
     *
     * @param transition
     * @return
     */
    boolean prepareTransition(Transition transition) {
        if (nextTransition != Transition.None) {
            addDebugInfo(() -> this + ": trying to prepareTransition " + transition + " on " + this + ", but we are already transitioning " + nextTransition);
            return false;
        }
        nextTransition = transition;
        addDebugInfo(() -> this + ": acquired lock for transition " + transition + " on " + this);
        return true;
    }

    /**
     * Determine whether the transition is allowed;
     * Typically: when a task completes with output parameters,
     * these get reflected back into the casefile. This in itself may trigger
     * a criterion, which may e.g. invoke a Terminate transition on the completing task, which then
     * coincides with the Complete transition. First the output parameter is set in the case file,
     * then the Task.complete transition is invoked. Completion of the task now invokes prepareTransition,
     * and thereby acquires a "transition lock", which then makes the terminating criterion on that task
     * await the completion. Or better: it does not await completion, but is simply ignored.
     *
     * @param transition
     * @return
     */
    private boolean hasLock(Transition transition) {
        if (nextTransition != Transition.None && !nextTransition.equals(transition)) {
            return false;
        }
        nextTransition = Transition.None;
        return true;
    }

    private Transition nextTransition = Transition.None;

    /**
     * Tries to make a transition
     *
     * @param transition
     */
    public boolean makeTransition(Transition transition) {
        if (!hasLock(transition)) { // First check to determine whether we are allowed to make this transition.
            addDebugInfo(() -> "StateMachine-" + this + ": cannot acquire lock for transition " + transition + " since currently transitioning " + nextTransition);
            return false;
        }
        PlanItemTransitioned event = stateMachine.transition(this, transition);
        if (event != null) { // means, a transition happened.
            addDebugInfo(() -> "StateMachine-" + this + ": allows transition: " + event.getHistoryState() + "." + event.getTransition().getValue() + "() ===> " + event.getCurrentState());
            addEvent(event);
            return true;
        } else {
            addDebugInfo(() -> "StateMachine-" + this + ": transition " + transition + " has no effect, current state remains " + getState());
            return false;
        }
    }

    private void setItemDefinition(ItemDefinition newItemDefinition) {
        this.previousItemDefinition = this.itemDefinition;
        this.itemDefinition = newItemDefinition;
    }

    public ItemDefinition getPreviousItemDefinition() {
        return previousItemDefinition;
    }

    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    /**
     * Returns the current state of the item
     *
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * Returns the previous state this item was in.
     *
     * @return
     */
    public State getHistoryState() {
        return historyState;
    }

    /**
     * Returns the last transition that this plan item went through
     *
     * @return
     */
    public Transition getLastTransition() {
        return lastTransition;
    }

    /**
     * To keep track of plan item history, every event generated by this plan item gets a sequence number that is unique _only_ wihtin the plan item.
     *
     * @return
     */
    public int getNextEventNumber() {
        return ++planItemEventCounter;
    }

    public boolean repeats() {
        return repetitionRuleOutcome;
    }

    /**
     * Returns the collection of entry criteria for this plan item
     *
     * @return
     */
    public PlanItemEntry getEntryCriteria() {
        return entryCriteria;
    }

    /**
     * Returns the collection of exit criteria for this plan item
     *
     * @return
     */
    public PlanItemExit getExitCriteria() {
        return exitCriteria;
    }

    /**
     * Evaluates the repetition rule on the plan item. Typically done when the plan item goes into Active state
     */
    void evaluateRepetitionRule(boolean firstEvaluation) {
        boolean newRuleOutcome = getItemDefinition().getPlanItemControl().getRepetitionRule().evaluate(this);
        if (firstEvaluation || newRuleOutcome != this.repetitionRuleOutcome) {
            addEvent(new RepetitionRuleEvaluated(this, newRuleOutcome));
        }
    }

    /**
     * Evaluates the required rule on the plan item. Typically done when the plan item goes into Active state
     */
    void evaluateRequiredRule() {
        boolean required = getItemDefinition().getPlanItemControl().getRequiredRule().evaluate(this);
        addEvent(new RequiredRuleEvaluated(this, required));
    }

    /**
     * Evaluates the manual activation rule on the plan item. Typically done when the plan item is created. Returns the required transition to be executed on the plan item: Enable if manual activation
     * resulted in true, Start when it resulted in false.
     */
    Transition evaluateManualActivationRule() {
        boolean manualActivation = getItemDefinition().getPlanItemControl().getManualActivationRule().evaluate(this);
        return manualActivation ? Transition.Enable : Transition.Start;
    }

    /**
     * Determines whether this plan item is required or not. Influences stage completion rules
     *
     * @return
     */
    public boolean isRequired() {
        return requiredRuleOutcome == true;
    }

    public void updateState(RepetitionRuleEvaluated event) {
        this.repetitionRuleOutcome = event.isRepeating();
    }

    public void updateState(RequiredRuleEvaluated event) {
        this.requiredRuleOutcome = event.isRequired();
    }

    public void publishTransition(PlanItemTransitioned event) {
        getPublisher().addEvent(event);
    }

    public void updateStandardEvent(PlanItemTransitioned event) {
        this.state = event.getCurrentState();
        this.historyState = event.getHistoryState();
        this.lastTransition = event.getTransition();
    }

    public void runStateMachineAction(PlanItemTransitioned event) {
        Transition transition = event.getTransition();
        State newState = event.getCurrentState();
        State oldState = event.getHistoryState();
        addDebugInfo(() -> this + ": handling transition '" + transition.getValue() + "' from " + oldState + " to " + newState);

        // First execute the related state machine action (e.g., activating a timer, releasing a subprocess, suspending, etc.etc.)
        StateMachine.Action action = stateMachine.getAction(newState);
        addDebugInfo(() -> "StateMachine-" + this + ": running action for state '" + newState + "'");
        action.execute(this, transition);
    }

    public void informConnectedEntryCriteria(PlanItemTransitioned event) {
        getPublisher().informEntryCriteria(event);
    }

    public void runStageCompletionCheck(PlanItemTransitioned event) {
        Transition transition = event.getTransition();
        State newState = event.getCurrentState();
        State oldState = event.getHistoryState();
        addDebugInfo(() -> this + ": handling transition '" + transition.getValue() + "' from " + oldState + " to " + newState);

        // Check stage completion (only done for transitions into semi terminal state)
        if (getStage() != null && newState.isSemiTerminal()) {
            getStage().tryCompletion(event);
        }
    }

    public void informConnectedExitCriteria(PlanItemTransitioned event) {
        // Finally iterate the terminating sentries and inform them
        getPublisher().informExitCriteria(event);

        addDebugInfo(() -> this + ": completed handling transition '" + event.getTransition().getValue() + "' from " + event.getHistoryState() + " to " + event.getCurrentState());
    }

    /**
     * Akka recovery method
     *
     * @param event
     */
    public void updateSequenceNumber(PlanItemEvent event) {
        this.planItemEventCounter = event.getSequenceNumber();
    }

    /**
     * Returns the id of the instance (which is the plan item id).
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the instance (which is the plan item name).
     *
     * @return
     */
    public String getName() {
        return getItemDefinition().getName();
    }

    /**
     * Returns getName() + dot + getIndex() in single quotes
     *
     * @return
     */
    public String getPath() {
        return "'" + this.getName() + "." + this.index + "'";
    }

    /**
     * Returns the repetition index of this plan item
     *
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the index of this plan item if it repeats, or else the plan item of the first surrounding stage that
     * repeats, or else 0.
     *
     * @return
     */
    public int getRepeatIndex() {
        return getRepeatIndex("");
    }

    protected int getRepeatIndex(String ancestorTree) {
        if (this.getItemDefinition().getPlanItemControl().getRepetitionRule().isDefault()) {
            if (this.getStage() != null) {
                return this.getStage().getRepeatIndex(this.getName() + "." + ancestorTree);
            } else {
                addDebugInfo(() -> "There is no repeating plan item in tree '" + this.getName() + "." + ancestorTree + "', returning index 0");
                return 0;
            }
        } else {
            addDebugInfo(() -> "Found repeat index '" + this.getName() + "[" + this.index + "]" + (ancestorTree.isBlank() ? "" : "." + ancestorTree) + "'");
            return this.index;
        }
    }

    /**
     * Returns the plan item type, e.g. CaseTask, Stage, UserEvent, etc.
     *
     * @return
     */
    public String getType() {
        return getDefinition().getType();
    }

    /**
     * Returns the stage to which this plan item belongs.
     *
     * @return
     */
    public Stage<?> getStage() {
        return stage;
    }

    @Override
    public String getDescription() {
        return toString();
    }

    final String toDescription() {
        return getType() + "[" + getName() + "|index=" + getIndex() + "|state=" + getState() + "|required=" + isRequired() + "|repeating=" + repeats() + "]";
    }

    public String toString() {
        return getType() + "[" + getName() + "." + index + "]";
    }

    protected void dumpImplementationToXML(Element planItemXML) {
    }

    protected void dumpMemoryStateToXML(Element parentElement) {
        Element planItemXML = parentElement.getOwnerDocument().createElement(this.getType());
        parentElement.appendChild(planItemXML);
        planItemXML.setAttribute("_id", this.getId());
        planItemXML.setAttribute("name", this.getName());
        if (repeats() || index > 0) {
            planItemXML.setAttribute("index", "" + index);
        }

        // Print the repeat attribute if there is a repetition rule defined for this plan item.
        if (!(this instanceof CasePlan) && !getItemDefinition().getPlanItemControl().getRepetitionRule().isDefault()) {
            planItemXML.setAttribute("repeat", "" + repeats());
        }

        planItemXML.setAttribute("state", "" + this.getState());
        planItemXML.setAttribute("transition", "" + this.getLastTransition());
        planItemXML.setAttribute("history", "" + this.getHistoryState());

        // Let instance append it's information.
        dumpImplementationToXML(planItemXML);

        entryCriteria.dumpMemoryStateToXML(planItemXML);
        exitCriteria.dumpMemoryStateToXML(planItemXML);
    }

    /**
     * Returns true if the plan item's parent is in active state.
     * Returns always true for the case plan.
     *
     * @return
     */
    public boolean hasActiveParent() {
        return getStage() == null || getStage().getState().isActive();
    }

    /**
     * Method that can be used by MakePlanItemTransition command to determine whether this plan item
     * can go through the suggested transition.
     * Checks whether the parent stage is in Active state.
     *
     * @param transition
     */
    public void validateTransition(Transition transition) {
        if (!hasActiveParent()) {
            throw new InvalidCommandException("Cannot perform action '" + transition + "' on '" + getName() + "', since the surrounding stage is not active");
        }
    }

    protected void createInstance() {
        evaluateRepetitionRule(true);
        evaluateRequiredRule();
        getEntryCriteria().beginLifeCycle();
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

    protected void retrieveDiscretionaryItems(Collection<DiscretionaryItem> items) {
    }

    /**
     * Indicates whether discretionary items are available for planning (applicable only for Stages and HumanTasks)
     *
     * @return false if it is a milestone or eventlistener
     */
    protected boolean hasDiscretionaryItems() {
        return false;
    }

    /**
     * Returns the entry transition (trigger when EntryCriterion is satisfied) for this type of plan item. Returns default {@link Transition#Start}, and
     * Milestone overrides this by returning {@link Transition#Occur}
     *
     * @return
     */
    abstract protected Transition getEntryTransition();

    /**
     * Returns the exit transition (trigger when ExitCriterion is satisfied) for this type of plan item. Returns default {@link Transition#Exit}, and
     * CasePlan overrides this by returning {@link Transition#Terminate}
     *
     * @return
     */
    final Transition getExitTransition() {
        return stateMachine.exitTransition;
    }

    /**
     * Transition to be made when parent stage terminates
     *
     * @return
     */
    final Transition getTerminationTransition() {
        return stateMachine.terminationTransition;
    }

    public void updateState(CaseAppliedPlatformUpdate event) {
    }

    protected void migrateItemDefinition(ItemDefinition newItemDefinition, T newDefinition) {
        addDebugInfo(() -> "=== Migrating " + this + " to a new definition");
        super.migrateDefinition(newDefinition);
        setItemDefinition(newItemDefinition);
        if (getState() != State.Null) {
            if (hasNewNameOrId()) {
                // Add a migration event if name or id has changed
                addEvent(new PlanItemMigrated(this));
            }
            // Check if there is a need to evaluate required rule again
            if (!getState().isSemiTerminal() || getState() == State.Failed) {
                if (hasNewRequiredRule()) {
                    evaluateRequiredRule();
                }
            }
        }
        getEntryCriteria().migrateCriteria(newItemDefinition);
        getExitCriteria().migrateCriteria(newItemDefinition);
        addDebugInfo(() -> "=== Completed migration of " + this + "\n");
    }

    private boolean hasNewNameOrId() {
        String oldName = getPreviousItemDefinition().getName();
        String oldId = getPreviousItemDefinition().getId();
        String newName = getItemDefinition().getName();
        String newId = getItemDefinition().getId();
        return !oldName.equals(newName) || !oldId.equals(newId);
    }

    private boolean hasNewRequiredRule() {
        ConstraintDefinition oldRequiredRule = getPreviousItemDefinition().getPlanItemControl().getRequiredRule();
        return getItemDefinition().getPlanItemControl().getRequiredRule().differs(oldRequiredRule);
    }

    @Override
    protected boolean hasNewDefinition() {
        // Note: We're overriding hasNewDefinition, since itemDefinition determines everything of a plan item
        //  This includes the reference to the actual definition like TaskDefinition or StageDefinition
        //  because these are referenced from the itemDefinition and included in the comparison
        return previousItemDefinition != null && itemDefinition.differs(previousItemDefinition);
    }

    protected void lostDefinition() {
        // Scenario not yet supported... What to do with existing items that cannot be found in the new stage definition?
        addDebugInfo(() -> "Dropping plan item " + getPath() + " upon case migration, as a new definition is not found for the plan item.");
        addEvent(new PlanItemDropped(this));
    }

    public void updateState(PlanItemDropped event) {
        getEntryCriteria().release();
        getExitCriteria().release();
        getStage().removeDroppedPlanItem(this);
        getCaseInstance().removeDroppedPlanItem(this);
    }
}
