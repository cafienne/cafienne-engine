/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.CaseAppliedPlatformUpdate;
import org.cafienne.cmmn.akka.event.plan.*;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.PlanItemDefinitionDefinition;
import org.cafienne.cmmn.instance.sentry.*;
import org.cafienne.util.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Collection;

public abstract class PlanItem<T extends PlanItemDefinitionDefinition> extends CMMNElement<T> {
    /**
     * Unique identifier of the plan item, typically a guid
     */
    private final String id;
    /**
     * If the plan item repeats, each item has 1 higher index (0, 1, 2, ...)
     */
    private final int index;
    /**
     * Name of the plan item, taken from the ItemDefinition
     */
    private final String name;
    /**
     * Type of plan item, taken from the PlanItemDefinitionDefinition (HumanTask, Stage, CaseTask, Milestone, etc)
     */
    private final String type;
    /**
     * Stage to which this plan item belongs (null for CasePlan)
     */
    private final Stage stage;
    /**
     * The actual plan item definition (or discretionary item definition, or caseplan definition).
     */
    private final ItemDefinition itemDefinition;
    /**
     * State machine that executes behavior
     */
    private final StateMachine stateMachine;

    /**
     * Our entry and exit criteria (i.e., to plan items and case file items of interest to us)
     */
    private final PlanItemEntry entryCriteria = new PlanItemEntry(this);
    private final PlanItemExit exitCriteria = new PlanItemExit(this);
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
    private final TransitionPublisher transitionPublisher = new TransitionPublisher(this);
    private Transition lastTransition = Transition.None;
    private State state = State.Null;
    private State historyState = State.Null;
    /**
     * Number of PlanItem events we have generated
     */
    private int planItemEventCounter = 0; // Akka event sequence number

    protected PlanItem(String id, int index, ItemDefinition itemDefinition, T definition, Stage parent, StateMachine stateMachine) {
        this(id, index, itemDefinition, definition, parent.getCaseInstance(), parent, stateMachine);
    }

    /**
     * Constructor for CasePlan (and Stages also use it)
     * @param id
     * @param index
     * @param itemDefinition
     * @param definition
     * @param caseInstance
     * @param parent
     * @param stateMachine
     */
    protected PlanItem(String id, int index, ItemDefinition itemDefinition, T definition, Case caseInstance, Stage parent, StateMachine stateMachine) {
        super(caseInstance, definition);
        this.id = id;
        this.itemDefinition = itemDefinition;
        this.stage = parent;
        this.name = itemDefinition.getName();
        this.type = definition.getType();
        this.index = index;
        this.stateMachine = stateMachine;

        addDebugInfo(() -> "Constructing plan item " + this + " with id " + id + (stage == null ? " in case" : " in "+stage));

        // Register at case level
        getCaseInstance().registerPlanItem(this);

        // Now connect to the rest of the network of plan items and sentries
        if (stage != null) {
            // Register with parent stage
            stage.register(this);
        }

        // Link ourselves to any existing sentries in the case
        getCaseInstance().getSentryNetwork().connect(this);
    }

    public void connectOnPart(PlanItemOnPart onPart) {
        transitionPublisher.connectOnPart(onPart);
    }

    public void releaseOnPart(PlanItemOnPart onPart) {
        transitionPublisher.releaseOnPart(onPart);
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
            if (itemDefinition.isDiscretionary()) {
                // Means we are discretionary, and adding to the plan must be done manually
                return;
            }
            // Generate an id for the repeat item
            String repeatItemId = new Guid().toString();
            // Create a new plan item
            addDebugInfo(() -> this + ": creating repeat item " + (index + 1) +" with id " + repeatItemId);
            PlanItemCreated pic = new PlanItemCreated(stage, getItemDefinition(), repeatItemId, index + 1);
            addEvent(pic);
            pic.getCreatedPlanItem().makeTransition(Transition.Create);
            pic.getCreatedPlanItem().makeTransition(getEntryTransition());
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
    public void makeTransition(Transition transition) {
        if (!hasLock(transition)) { // First check to determine whether we are allowed to make this transition.
            addDebugInfo(() -> "StateMachine-"+this+": cannot acquire lock for transition " + transition + " since currently transitioning " + nextTransition);
            return;
        }
        PlanItemTransitioned event = stateMachine.transition(this, transition);
        if (event != null) { // means, a transition happened.
            addDebugInfo(() -> "StateMachine-"+this+": allows transition: " + event.getHistoryState() + "." + event.getTransition().getValue() + "() ===> " + event.getCurrentState());
            addEvent(event);
        } else {
            addDebugInfo(() -> "StateMachine-"+this+": transition " + transition + " has no effect, current state remains " + getState());
        }
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
        boolean newRuleOutcome = itemDefinition.getPlanItemControl().getRepetitionRule().evaluate(this);
        if (firstEvaluation || newRuleOutcome != this.repetitionRuleOutcome) {
            addEvent(new RepetitionRuleEvaluated(this, newRuleOutcome));
        }
    }

    /**
     * Evaluates the required rule on the plan item. Typically done when the plan item goes into Active state
     */
    void evaluateRequiredRule() {
        boolean required = itemDefinition.getPlanItemControl().getRequiredRule().evaluate(this);
        addEvent(new RequiredRuleEvaluated(this, required));
    }

    /**
     * Evaluates the manual activation rule on the plan item. Typically done when the plan item is created. Returns the required transition to be executed on the plan item: Enable if manual activation
     * resulted in true, Start when it resulted in false.
     */
    Transition evaluateManualActivationRule() {
        boolean manualActivation = itemDefinition.getPlanItemControl().getManualActivationRule().evaluate(this);
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

    public void updateState(PlanItemTransitioned event) {
        this.transitionPublisher.addEvent(event);
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
        addDebugInfo(() -> "StateMachine-"+this+": running action for state '"+newState+"'");
        action.execute(this, transition);
    }

    public void informConnectedEntryCriteria(PlanItemTransitioned event) {
        transitionPublisher.informEntryCriteria(event);
    }

    public void runStageCompletionCheck(PlanItemTransitioned event) {
        Transition transition = event.getTransition();
        State newState = event.getCurrentState();
        State oldState = event.getHistoryState();
        addDebugInfo(() -> this + ": handling transition '" + transition.getValue() + "' from " + oldState + " to " + newState);

        // Check stage completion (only done for transitions into semi terminal state)
        if (stage != null && newState.isSemiTerminal()) {
            stage.tryCompletion(event);
        }
    }

    public void informConnectedExitCriteria(PlanItemTransitioned event) {
        // Finally iterate the terminating sentries and inform them
        transitionPublisher.informExitCriteria(event);

        addDebugInfo(() -> this + ": completed handling transition '"+event.getTransition().getValue()+"' from " + event.getHistoryState() + " to " + event.getCurrentState());
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
     * @return
     */
    public String getName() {
        return name;
    }

    /**
      * Returns getName() + dot + getIndex() in single quotes
      * @return
      */
    public String getPath() {
        return "'" + this.name +"." + this.index +"'";
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
     * Returns the plan item type, e.g. CaseTask, Stage, UserEvent, etc.
     *
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the stage to which this plan item belongs.
     *
     * @return
     */
    public Stage getStage() {
        return stage;
    }

    final String toDescription() {
        return getType() + "[" + getName() +"|index=" + getIndex()+"|state="+getState()+"|required="+isRequired()+"|repeating="+repeats()+"]";
    }

    public String toString() {
        return type + "[" + getName() + "." + index + "]";
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
        if (! (this instanceof CasePlan) && !itemDefinition.getPlanItemControl().getRepetitionRule().isDefault()) {
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

    protected void retrieveDiscretionaryItems(Collection<DiscretionaryItem> items) {
    }

    /**
     * Indicates whether discretionary items are available for planning (applicable only for Stages and HumanTasks)
     * @return false if it is a milestone or eventlistener
     */
    protected boolean hasDiscretionaryItems() {
        return false;
    }

    /**
     * Returns the entry transition (trigger when EntryCriterion is satisfied) for this type of plan item. Returns default {@link Transition#Start}, and
     * Milestone overrides this by returning {@link Transition#Occur}
     * @return
     */
    final Transition getEntryTransition() {
        return stateMachine.entryTransition;
    }

    /**
     * Returns the exit transition (trigger when ExitCriterion is satisfied) for this type of plan item. Returns default {@link Transition#Exit}, and
     * CasePlan overrides this by returning {@link Transition#Terminate}
     * @return
     */
    final Transition getExitTransition() {
        return stateMachine.exitTransition;
    }

    /**
     * Transition to be made when parent stage terminates
     * @return
     */
    final Transition getTerminationTransition() {
        return stateMachine.terminationTransition;
    }

    public void updateState(CaseAppliedPlatformUpdate event) {
    }
}
