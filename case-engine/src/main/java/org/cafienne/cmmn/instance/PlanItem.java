/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.plan.*;
import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.cafienne.cmmn.instance.sentry.ExitCriterion;
import org.cafienne.cmmn.instance.sentry.PlanItemOnPart;
import org.cafienne.util.Guid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlanItem extends CMMNElement<PlanItemDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(PlanItem.class);

    private final String id;
    private final String name;
    private final String type;
    private boolean repetitionRuleOutcome;
    private boolean requiredRuleOutcome;
    private Transition lastTransition = Transition.None;
    private State state = State.Null;
    private State historyState = State.Null;
    private int planItemEventCounter = 0; // Akka event sequence number
    private int index;

    private Stage<?> stage;
    private PlanItemDefinitionInstance<?> instance;
    private StateMachine stateMachine; // State machine that executes our behavior
    private final List<PlanItemOnPart> connectedEntryCriteria = new ArrayList<PlanItemOnPart>();
    private final List<PlanItemOnPart> connectedExitCriteria = new ArrayList<PlanItemOnPart>();
    private final Collection<EntryCriterion> entryCriteria = new ArrayList<>();
    private final Collection<ExitCriterion> exitCriteria = new ArrayList<>();

    private DiscretionaryItemDefinition def; // Yilk, this is ugly. Need to come up with something better!

    /**
     * Constructor for adding discretionary items to the plan
     */
    private PlanItem(String id, DiscretionaryItemDefinition definition, PlanItemDefinitionDefinition reference, Case caseInstance, Stage<?> owningStage, int index) {
        this(id, definition.getName(), definition.getEntryCriteria(), definition.getExitCriteria(), null, definition, reference, caseInstance, owningStage, index);
    }

    /**
     * Constructor for adding plan items to the plan
     */
    private PlanItem(String id, PlanItemDefinition definition, PlanItemDefinitionDefinition reference, Case caseInstance, Stage<?> owningStage, int index) {
        this(id, definition.getName(), definition.getEntryCriteria(), definition.getExitCriteria(), definition, null, reference, caseInstance, owningStage, index);
    }

    /**
     * Constructor for adding case plan
     */
    private PlanItem(String id, CasePlanDefinition reference, Case caseInstance) {
        this(id, reference.getName(), null, reference.getExitCriteria(), null, null, reference, caseInstance, null, 0);
    }

    public static PlanItem create(Case actor, PlanItemCreated event) {
        String stageId = event.getStageId();
        if (stageId.isEmpty()) {
            CasePlanDefinition definition = actor.getDefinition().getCasePlanModel();
            PlanItem planItem = new PlanItem(event.planItemId, definition, actor);
            actor.setCasePlan(planItem);
            return planItem;
        } else {
            // Lookup the stage to which the plan item belongs,
            // then lookup the definition for the plan item
            // and then instantiate it.
            PlanItem owningPlanItem = actor.getPlanItemById(stageId);
            if (owningPlanItem == null) {
                logger.error("MAJOR ERROR: we cannot find the stage with id " + stageId + ", and therefore cannot recover plan item " + event);
                return null;
            }

            Stage<?> stage = owningPlanItem.getInstance();
            PlanItemDefinition definition = stage.getDefinition().getPlanItem(event.planItemName);
            // If definition == null, try to see if it's a discretionaryItem
            if (definition == null) {
                DiscretionaryItemDefinition diDefinition = stage.getDefinition().getDiscretionaryItem(event.planItemName);
                if (diDefinition == null) {
                    logger.error("MAJOR ERROR: we cannot find a plan item definition named '" + event.planItemName + "' in stage " + event.getStageId() + ", and therefore cannot recover plan item " + event);
                }
                PlanItemDefinitionDefinition reference = diDefinition.getPlanItemDefinition();
                return new PlanItem(event.planItemId, diDefinition, reference, actor, stage, event.index);
            } else {
                PlanItemDefinitionDefinition reference = definition.getPlanItemDefinition();
                return new PlanItem(event.planItemId, definition, reference, actor, stage, event.index);
            }
        }
    }

    private PlanItem(String id, String name, Collection<EntryCriterionDefinition> definedEntryCriteria, Collection<ExitCriterionDefinition> definedExitCriteria, PlanItemDefinition planItemDefinition,
                     DiscretionaryItemDefinition discretionaryDefinition, PlanItemDefinitionDefinition reference, Case caseInstance, Stage<?> owningStage, int index) {
        super(caseInstance, planItemDefinition);
        this.id = id;
        this.def = discretionaryDefinition;
        this.stage = owningStage;
        this.name = name;
        this.type = reference.getType();
        this.index = index;

        addDebugInfo(() -> "Constructing plan item " + this + " with id " + id + (stage == null ? " in case" : " in "+stage));

        // Create our typed instance (task / stage / milestone / eventlistener)
        this.instance = reference.createInstance(this);

        // Register at case level
        getCaseInstance().registerPlanItem(this);

        // Now connect to the rest of the network of plan items and sentries
        if (stage != null) {
            // Register with parent stage
            stage.getPlanItems().add(this);

            // Link ourselves to any existing sentries in the case
            getCaseInstance().getSentryNetwork().connect(this);

            // Create new sentries within the case to which we will react
            definedEntryCriteria.forEach(c -> entryCriteria.add(stage.getEntryCriterion(c)));
            definedExitCriteria.forEach(c -> exitCriteria.add(stage.getExitCriterion(c)));
        } else { // We are the case plan, so make sure we connect the exit criteria
            CasePlan casePlan = getInstance();
            definedExitCriteria.forEach(c -> exitCriteria.add(casePlan.getExitCriterion(c)));
        }
    }

    public void connectOnPart(PlanItemOnPart onPart) {
        if (onPart.getSentry().isEntryCriterion()) {
            insertOnPart(onPart, connectedEntryCriteria);
        } else {
            insertOnPart(onPart, connectedExitCriteria);
        }
        onPart.inform(this);
    }

    /**
     * Inserts the onPart in the right location of the plan item hierarchy
     *
     * @param onPart
     * @param list
     */
    private void insertOnPart(PlanItemOnPart onPart, List<PlanItemOnPart> list) {
        if (list.contains(onPart)) {
            return; // do not connect more than once
        }
        PlanItem onPartStage = onPart.getSentry().getStage().getPlanItem();
        int i = 0;
        // Iterate the list until we encounter an onPart that does not contain the new sentry.
        while (i < list.size() && list.get(i).getSentry().getStage().contains(onPartStage)) {
            i++;
        }
        list.add(i, onPart);
    }

    /**
     * Returns the (database) identifier for this plan item.
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the plan item.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the typed instance for this plan item (e.g., a HumanTask, a Stage, a Milestone, etc.) The type is cast to the generic for ease of coding
     *
     * @return
     */
    public <T extends PlanItemDefinitionInstance<?>> T getInstance() {
        // For convenience of callers we are doing the cast
        @SuppressWarnings("unchecked")
        T typedInstance = (T) instance;
        return typedInstance;
    }

    /**
     * Returns the stage to which this plan item belongs.
     *
     * @return
     */
    public Stage<?> getStage() {
        return stage;
    }

    /**
     * Starts the plan item's lifecycle (basically triggers Transition.Create)
     */
    public void beginLifecycle() {
        addDebugInfo(() -> this + ": starting lifecycle");
        makeTransition(Transition.Create);
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
     * a sentry, which may e.g. invoke a Terminate transition on the completing task, which then
     * coincides with the Complete transition. First the output parameter is set in the case file,
     * then the Task.complete transition is invoked. Completion of the task now invokes prepareTransition,
     * and thereby acquires a "transition lock", which then makes the terminating sentry on that task
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
            addDebugInfo(() -> this + ": cannot acquire lock for transition " + transition + " since currently transitioning " + nextTransition);
            return;
        }
        PlanItemTransitioned event = stateMachine.transition(this, transition);
        if (event != null) { // means, a transition happened.
            addDebugInfo(() -> this + ".StateMachine makes transition: " + event);
            getCaseInstance().addEvent(event);
        } else {
            addDebugInfo(() -> this + ".StateMachine: transition " + transition + " has no effect, current state remains " + getState());
        }
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

    /**
     * Returns the plan item type, e.g. CaseTask, Stage, UserEvent, etc.
     *
     * @return
     */
    public String getType() {
        return type;
    }

    public boolean repeats() {
        return repetitionRuleOutcome;
    }

    void setStateMachine(StateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Returns the collection of instantiated sentries that form the entry criteria for this plan item
     *
     * @return
     */
    public Collection<EntryCriterion> getEntryCriteria() {
        return entryCriteria;
    }

    /**
     * Returns the collection of instantiated sentries that form the exit criteria of this plan item
     *
     * @return
     */
    public Collection<ExitCriterion> getExitCriteria() {
        return exitCriteria;
    }

    /**
     * Evaluates the repetition rule on the plan item. Typically done when the plan item goes into Active state
     */
    void evaluateRepetitionRule() {
        boolean repeats = getPlanItemControl().getRepetitionRule().evaluate(this);
        getCaseInstance().addEvent(new RepetitionRuleEvaluated(this, repeats));
    }

    /**
     * Evaluates the required rule on the plan item. Typically done when the plan item goes into Active state
     */
    void evaluateRequiredRule() {
        boolean required = getPlanItemControl().getRequiredRule().evaluate(this);
        getCaseInstance().addEvent(new RequiredRuleEvaluated(this, required));
    }

    /**
     * Evaluates the manual activation rule on the plan item. Typically done when the plan item is created. Returns the required transition to be executed on the plan item: Enable if manual activation
     * resulted in true, Start when it resulted in false.
     */
    Transition evaluateManualActivationRule() {
        boolean manualActivation = getPlanItemControl().getManualActivationRule().evaluate(this);
        return manualActivation ? Transition.Enable : Transition.Start;
    }

    ItemControlDefinition getPlanItemControl() {
        if (def != null) {
            return def.getPlanItemControl();
        }
        return getDefinition().getPlanItemControl();
    }

    /**
     * Determines whether this plan item is required or not. Influences stage completion rules
     *
     * @return
     */
    public boolean isRequired() {
        return requiredRuleOutcome == true;
    }

    /**
     * Repeats the plan item upon it's Completion or Termination, and only if there are no entry criteria.
     * Additionally checks that the containing stage is still active.
     */
    public void repeat() {
        addDebugInfo(() -> this + ": initiating repeat logic for next item");

        // Repeat the plan item when it is in Completed or Terminated state - Or if it has entry criteria being met
        if (getStage().getPlanItem().getState() != State.Active) {
            // The stage that contains us is no longer active. So we will not prepare any repeat item.
            // This code is typically invoked if the the stage terminates or completes, causing the repeating element to terminate.
            addDebugInfo(() -> this + ": not repeating because stage is not active " + getName());
            return;
        }

        // Re-evaluate the repetition rule, and if the outcome is true, the start the repeat item (but only if it is not a discretionary).
        addDebugInfo(() -> this + ": evaluating RepetitionRule from within repeat() ");
        evaluateRepetitionRule();
        if (repeats()) {
            if (getDefinition() == null) {
                // Means we are discretionary, and adding to the plan must be done manually
                return;
            }
            // Generate an id for the repeat item
            String repeatItemId = new Guid().toString();
            // Create a new plan item
            addDebugInfo(() -> this + ": creating repeat item " + (index + 1) +" with id " + repeatItemId);
            getCaseInstance().addEvent(new PlanItemCreated(stage, getDefinition(), repeatItemId, index + 1));
        }
    }

    /**
     * Method invoked by the various state machines upon creation of the plan item;
     * typically determines whether it must be started or should wait for entry criteria to become active
     *
     * @param transition
     */
    void checkEntryCriteria(Transition transition) {
        addDebugInfo(() -> this + ": dhecking EntryCriteria in " + getName() + " with transition " + transition);
        if (getEntryCriteria().size() == 0) { // No entry criteria means get started immediately
            addDebugInfo(() -> this + ": no EntryCriteria found, making transition " + transition);
            makeTransition(transition);
        } else {
            // If the plan item is being repeated, then the entry criteria have been satisfied already;
            // avoid keep repeating ourselves
            if (index > 0) {
                addDebugInfo(() -> this + ": Not making transition because this is a repeat item");
                return;
            }
            // Evaluate sentries to see whether one is already active, and, if so, make the transition
            for (EntryCriterion criterion : getEntryCriteria()) {
                if (criterion.isSatisfied()) {
                    addDebugInfo(() -> this + ": an EntryCriterion is satisfied, making transition " + transition);
                    makeTransition(transition);
                    return;
                }
            }
            addDebugInfo(() -> this + ": Not making transition because no entry criteria are satisfied");
        }
    }

    public void updateState(RepetitionRuleEvaluated event) {
        this.repetitionRuleOutcome = event.isRepeating();
    }

    public void updateState(RequiredRuleEvaluated event) {
        this.requiredRuleOutcome = event.isRequired();
    }

    public void updateState(PlanItemTransitioned event) {
        this.state = event.getCurrentState();
        this.historyState = event.getHistoryState();
        this.lastTransition = event.getTransition();
    }

    public void runBehavior(PlanItemTransitioned pit) {
        Transition transition = pit.getTransition();
        State newState = pit.getCurrentState();
        State oldState = pit.getHistoryState();
        addDebugInfo(() -> this + ": handling transition '"+transition.getValue()+"' from " + oldState + " to " + newState);

        // First execute the related state machine action (e.g., activating a timer, releasing a subprocess, suspending, etc.etc.)
        StateMachine.Action action = stateMachine.getAction(newState);
        action.execute(this, transition);

        // Then inform the activating sentries
        connectedEntryCriteria.forEach(onPart -> onPart.inform(this));

        // Now check stage completion; but only if we're in semi terminal state (and of course if we are in a stage).
        if (stage != null && state.isSemiTerminal()) {
            stage.tryCompletion(this, transition);
        }

        // Finally iterate the terminating sentries and inform them
        connectedExitCriteria.forEach(onPart -> onPart.inform(this));

        addDebugInfo(() -> this + ": completed handling transition '"+transition.getValue()+"' from " + oldState + " to " + newState);
    }

    /**
     * Akka recovery method
     *
     * @param event
     */
    public void updateSequenceNumber(PlanItemEvent event) {
        this.planItemEventCounter = event.getSequenceNumber();
    }

    @Override
    public String toString() {
        return type + "[" + getName() + "." + index + "]";
    }

    public void dumpMemoryStateToXML(Element parentElement) {
        Element planItemXML = parentElement.getOwnerDocument().createElement(this.getType());
        parentElement.appendChild(planItemXML);
        planItemXML.setAttribute("_id", this.getId());
        planItemXML.setAttribute("name", this.getName());
        if (repeats() || index > 0) {
            planItemXML.setAttribute("index", "" + index);
        }

        // Print the repeat attribute if there is a repetition rule defined for this plan item.
        if (getDefinition() != null && !getPlanItemControl().getRepetitionRule().isDefault()) {
            planItemXML.setAttribute("repeat", "" + repeats());
        }

        planItemXML.setAttribute("state", "" + this.getState());
        planItemXML.setAttribute("transition", "" + this.getLastTransition());
        planItemXML.setAttribute("history", "" + this.getHistoryState());

        // Let instance append it's information.
        instance.dumpMemoryStateToXML(planItemXML);

        if (!getEntryCriteria().isEmpty()) {
            planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Entry criteria "));
            for (EntryCriterion criterion : getEntryCriteria()) {
                criterion.dumpMemoryStateToXML(planItemXML, true);
            }
        }
        if (!getExitCriteria().isEmpty()) {
            planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Exit criteria "));
            for (ExitCriterion criterion : getExitCriteria()) {
                criterion.dumpMemoryStateToXML(planItemXML, true);
            }
        }

        if (!connectedEntryCriteria.isEmpty()) {
            planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Listening sentries that will be informed before stage completion check "));
            connectedEntryCriteria.forEach(onPart -> onPart.getSentry().dumpMemoryStateToXML(planItemXML, false));
        }

        if (!connectedExitCriteria.isEmpty()) {
            planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Listening sentries that will be informed after stage completion check "));
            connectedExitCriteria.forEach(onPart -> onPart.getSentry().dumpMemoryStateToXML(planItemXML, false));
        }
    }

    /**
     * Returns the repetition index of this plan item
     *
     * @return
     */
    public int getIndex() {
        return index;
    }
}
