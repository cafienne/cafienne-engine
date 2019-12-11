/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.cafienne.akka.actor.command.exception.CommandException;

/**
 * Simple state machine logic, with an indirection to figure out where we are and where we go
 */
class StateMachine {
    private final Map<State, Map<Transition, Target>> transitions = new HashMap<State, Map<Transition, Target>>();
    private final Map<State, Target> states = new HashMap<State, Target>();

    private StateMachine() {
        // Register all states by default.
        for (State state : State.values()) {
            getTarget(state);
            getTransitions(state);
        }
    }

    /**
     * Configures a possible transition from one state to the other.
     *
     * @param transition
     * @param fromState
     * @param targetState
     */
    private void addTransition(Transition transition, State fromState, State targetState) {
        Map<Transition, Target> stateTransitions = getTransitions(fromState);
        stateTransitions.put(transition, getTarget(targetState));
    }

    /**
     * Configures a possible transition from a set of states to a target state.
     *
     * @param transition
     * @param fromState
     * @param targetState
     */
    private void addTransition(Transition transition, State[] fromStates, State targetState) {
        for (State fromState : fromStates) {
            addTransition(transition, fromState, targetState);
        }
    }

    /**
     * Configures the action that will be executed if an instance of type T enters the given state
     *
     * @param state
     * @param action
     */
    private void setAction(State state, Action action) {
        getTarget(state).action = action;
    }

    /**
     * Target state wrapper
     *
     * @param state
     * @return
     */
    private Target getTarget(State state) {
        Target target = states.get(state);
        if (target == null) {
            target = new Target(state);
            states.put(state, target);
        }
        return target;
    }

    private Map<Transition, Target> getTransitions(State state) {
        Map<Transition, Target> stateTransitions = transitions.get(state);
        if (stateTransitions == null) {
            stateTransitions = new HashMap<Transition, Target>();
            transitions.put(state, stateTransitions);
        }
        return stateTransitions;
    }

    /**
     * Make a transition on the instance.
     * Returns true if the transition resulted in a state change, false if the state remained the same.
     *
     * @param planItem
     * @param transition
     * @return
     */
    Action transition(PlanItem planItem, Transition transition) {
        State currentState = planItem.getState();
        Map<Transition, Target> stateTransitions = transitions.get(currentState);
        Target target = stateTransitions.get(transition);
        if (target != null) {
            if (target.state == null) {
                // If the target state is undefined, it means we to go back to the history state. Only instance knows it's history state.
                // We have to fetch the target again, based on the history state, as that will contain the proper action
                target = getTarget(planItem.getHistoryState());
            }
            if (!planItem.getInstance().isTransitionAllowed(transition)) // Evaluate the guard (if any)
            {
                return null;
            }
            planItem.setState(target.state, currentState, transition);
            return target.action;
        } else {
            return null; // no transition
        }
    }

    private class Target {
        private final State state;
        private Action action;

        private Target(State targetState) {
            this.state = targetState;
            this.action = (PlanItem p, Transition t) -> {
            }; // By default an empty action.
        }
    }

    /**
     * Action to be executed when a plan item has entered a state
     */
    interface Action // To be executed once a state is entered
    {
        /**
         * @param planItem The plan item that has made the state change
         * @param transition The transition that caused the state change
         */
        void execute(PlanItem planItem, Transition transition);
    }

    // State machine configuration for events and milestones
    static final StateMachine EventMilestone = new StateMachine();

    static {
        EventMilestone.addTransition(Transition.Create, State.Null, State.Available);
        EventMilestone.addTransition(Transition.Suspend, State.Available, State.Suspended);
        EventMilestone.addTransition(Transition.ParentSuspend, State.Available, State.Suspended);
        EventMilestone.addTransition(Transition.Terminate, State.Available, State.Terminated);
        EventMilestone.addTransition(Transition.Occur, State.Available, State.Completed);
        EventMilestone.addTransition(Transition.Resume, State.Suspended, State.Available);
        EventMilestone.addTransition(Transition.ParentResume, State.Suspended, State.Available);
        EventMilestone.addTransition(Transition.ParentTerminate, new State[] { State.Available, State.Suspended }, State.Terminated);

        EventMilestone.setAction(State.Terminated, (PlanItem p, Transition t) -> p.getInstance().terminateInstance());
        EventMilestone.setAction(State.Suspended, (PlanItem p, Transition t) -> p.getInstance().suspendInstance());
        EventMilestone.setAction(State.Available, (PlanItem p, Transition t) -> {
            if (t == Transition.Create) {
                p.getInstance().createInstance();
                if (p.getInstance() instanceof Milestone) {
                    p.evaluateRepetitionRule();
                    p.evaluateRequiredRule();
                    p.checkEntryCriteria(Transition.Occur);
                }
            } else if (t == Transition.Resume || t == Transition.ParentResume) {
                p.getInstance().resumeInstance();
            }
        });
    }

    // State machine configuration for tasks and stages
    static final StateMachine TaskStage = new StateMachine();

    static {
        TaskStage.addTransition(Transition.Create, State.Null, State.Available);
        TaskStage.addTransition(Transition.Enable, State.Available, State.Enabled);
        TaskStage.addTransition(Transition.Start, State.Available, State.Active);
        TaskStage.addTransition(Transition.Disable, State.Enabled, State.Disabled);
        TaskStage.addTransition(Transition.ManualStart, State.Enabled, State.Active);
        TaskStage.addTransition(Transition.Suspend, State.Active, State.Suspended);
        TaskStage.addTransition(Transition.Fault, State.Active, State.Failed);
        TaskStage.addTransition(Transition.Complete, State.Active, State.Completed);
        TaskStage.addTransition(Transition.Terminate, State.Active, State.Terminated);
        TaskStage.addTransition(Transition.Exit, new State[] { State.Available, State.Active, State.Enabled, State.Disabled, State.Suspended, State.Failed }, State.Terminated);
        TaskStage.addTransition(Transition.Resume, State.Suspended, State.Active);
        TaskStage.addTransition(Transition.Reactivate, State.Failed, State.Active);
        TaskStage.addTransition(Transition.Reenable, State.Disabled, State.Enabled);
        TaskStage.addTransition(Transition.ParentSuspend, new State[] { State.Available, State.Active, State.Enabled, State.Disabled }, State.Suspended);
        TaskStage.addTransition(Transition.ParentResume, State.Suspended, null);

        TaskStage.setAction(State.Available, (PlanItem p, Transition t) -> {
            p.getInstance().createInstance();
            p.evaluateRepetitionRule();
            p.evaluateRequiredRule();

            // Now evaluate manual activation and trigger the associated transition on the plan item
            Transition transition = p.evaluateManualActivationRule();
            p.checkEntryCriteria(transition);
        });
        TaskStage.setAction(State.Active, (PlanItem p, Transition t) -> {
            if (t == Transition.Start || t == Transition.ManualStart) {
                p.getInstance().startInstance();
            } else if (t == Transition.Resume || t == Transition.ParentResume) {
                p.getInstance().resumeInstance();
            } else if (t == Transition.Reactivate) {
                p.getInstance().reactivateInstance();
            } else {
                // Ignoring it...; but for now throw an exception to see if we ever run into this code.
                throw new CommandException("FIRST TIME EXCEPTION: I am an unexpected transition on this stage/task");
            }
        });
        TaskStage.setAction(State.Enabled, (PlanItem p, Transition t) -> p.makeTransition(Transition.Start));
        TaskStage.setAction(State.Suspended, (PlanItem p, Transition t) -> p.getInstance().suspendInstance());
        TaskStage.setAction(State.Completed, (PlanItem p, Transition t) -> {
            p.getInstance().completeInstance();
            if (p.getEntryCriteria().isEmpty()) {
                p.repeat();
            }
        });
        TaskStage.setAction(State.Terminated, (PlanItem p, Transition t) -> {
            p.getInstance().terminateInstance();
            if (p.getEntryCriteria().isEmpty()) {
                p.repeat();
            }
        });
        TaskStage.setAction(State.Failed, (PlanItem p, Transition t) -> p.getInstance().failInstance());
    }

    // State machine configuration for the case plan
    static final StateMachine CasePlan = new StateMachine();

    static {
        CasePlan.addTransition(Transition.Create, State.Null, State.Active);
        CasePlan.addTransition(Transition.Suspend, State.Active, State.Suspended);
        CasePlan.addTransition(Transition.Terminate, State.Active, State.Terminated);
        CasePlan.addTransition(Transition.Complete, State.Active, State.Completed);
        CasePlan.addTransition(Transition.Fault, State.Active, State.Failed);
        CasePlan.addTransition(Transition.Reactivate, new State[] { State.Completed, State.Terminated, State.Failed, State.Suspended }, State.Active);
        CasePlan.addTransition(Transition.Close, new State[] { State.Completed, State.Terminated, State.Failed, State.Suspended }, State.Closed);

        CasePlan.setAction(State.Suspended, (PlanItem p, Transition t) -> p.getInstance().suspendInstance());
        CasePlan.setAction(State.Completed, (PlanItem p, Transition t) -> p.getInstance().completeInstance());
        CasePlan.setAction(State.Terminated, (PlanItem p, Transition t) -> p.getInstance().terminateInstance());
        CasePlan.setAction(State.Failed, (PlanItem p, Transition t) -> p.getInstance().failInstance());
        CasePlan.setAction(State.Active, (PlanItem p, Transition t) -> {
            if (t == Transition.Create) {
                // Create plan items
                p.getInstance().startInstance();
            } else {
                p.getInstance().resumeInstance();
            }
        });
    }
}
