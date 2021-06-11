/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.actorapi.event.plan.RepetitionRuleEvaluated;
import org.cafienne.cmmn.actorapi.event.plan.RequiredRuleEvaluated;
import org.cafienne.cmmn.actorapi.event.plan.PlanItemEvent;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.test.CaseTestCommand;

import java.util.Collections;
import java.util.List;

/**
 * Some basic assertions for plan items
 */
public class PlanItemAssertion extends ModelTestCommandAssertion<CaseTestCommand> {
    protected final String caseId;

    private final String id;
    private final String name;
    private final String type;
    private final String description;
    private final List<PlanItemEvent> events;

    PlanItemAssertion(CaseTestCommand command, PlanItemCreated pic) {
        super(command);
        this.caseId = command.getCaseInstanceId();
        this.id = pic.getPlanItemId();
        this.name = pic.getPlanItemName();
        this.type = pic.getType();
        this.description = type + " '" + name + "' with id "+id;

        PublishedEventsAssertion<PlanItemEvent> allPlanItemEvents = command.getEventListener().getEvents().filter(PlanItemEvent.class);
        this.events = allPlanItemEvents.filter(pie -> pie.getPlanItemId().equals(this.id)).getEvents();
        Collections.reverse(this.events);// Reverse order the events, such that last one comes first.
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    protected <T extends PlanItem<?>> PlanItemAssertion assertType(Class<T> ...typeClasses) {
        for (int i=0; i<typeClasses.length; i++) {
            if (this.type.equals(typeClasses[i].getSimpleName())) {
                return this;
            }
        }
        throw new AssertionError("Plan item " + name + " is of type " + type + ", but that is not as expected");
    }

    /**
     * Asserts that the plan item's instance has the expected type (e.g., Task, Stage, Milestone)
     *
     * @param typeClass
     */
    public <T extends PlanItem<?>> PlanItemAssertion assertType(Class<T> typeClass) {
        if (!type.equals(typeClass.getSimpleName())) {
            throw new AssertionError("Plan item " + name + " is of type " + type + ", but is expected to be of type " + typeClass.getSimpleName());
        }
        return this;
    }

    /**
     * Asserts that the plan item is in the expected state.
     *
     * @param expectedState
     */
    public PlanItemAssertion assertState(State expectedState) {
        if (expectedState == State.Null) {
            // There should not be any events other than PlanItemCreated yet.
            if (this.events.size() == 1) {
                return this;
            }
            throw new AssertionError(description + "is not in state Null, but in state "+getState());
        }
        if (getState() != expectedState) {
            throw new AssertionError(description + " is not in state " + expectedState + " but in state " + getState());
        }
        return this;
    }

    private PlanItemTransitioned getLastTransition() {
        return getLast(PlanItemTransitioned.class);
    }

    /**
     * Asserts the last transition on the plan item to be equal to the expected transition
     *
     * @param expectedTransition
     */
    public PlanItemAssertion assertLastTransition(Transition expectedTransition) {
        return assertLastTransition(expectedTransition, getState(), getHistoryState());
    }

    public State getState() {
        return getLastTransition().getCurrentState();
    }

    public State getHistoryState() {
        return getLastTransition().getHistoryState();
    }

    public Transition getTransition() {
        return getLastTransition().getTransition();
    }

    /**
     * Asserts the last transition on the plan item, along with expected current state and history state
     *
     * @param expectedTransition
     * @param expectedState
     * @param expectedHistoryState
     */
    public PlanItemAssertion assertLastTransition(Transition expectedTransition, State expectedState, State expectedHistoryState) {
        if (!getTransition().equals(expectedTransition)) {
            throw new AssertionError(description + " did not make transition " + expectedTransition + " but " + getTransition());
        }
        if (!getState().equals(expectedState)) {
            throw new AssertionError(description + " is not in state " + expectedState + " but in state " + getState());
        }
        if (!getHistoryState().equals(expectedHistoryState)) {
            throw new AssertionError(description + " does not have history state " + expectedHistoryState + " but " + getHistoryState());
        }
        return this;
    }

    /**
     * Assertion on the outcome of the evaluation of the repetition rule
     *
     * @param expectedOutcome
     * @return
     */
    public PlanItemAssertion assertRepeats(boolean expectedOutcome) {
        boolean planItemRepeats = getLast(RepetitionRuleEvaluated.class).isRepeating();
        if (planItemRepeats != expectedOutcome) {
            if (planItemRepeats) {
                throw new AssertionError(description + " is not expected to repeat, but it repeats");
            } else {
                throw new AssertionError(description + " is expected to repeat, but it doesn't");
            }
        }
        return this;
    }

    /**
     * Assertion on the outcome of the evaluation of the repetition rule to be true
     *
     * @return
     */
    public PlanItemAssertion assertRepeats() {
        return assertRepeats(true);
    }

    /**
     * Assertion on the outcome of the evaluation of the repetition rule to be false
     *
     * @return
     */
    public PlanItemAssertion assertNoRepetition() {
        return assertRepeats(false);
    }

    /**
     * Assertion on the outcome of the evaluation of the required rule
     *
     * @param expectedOutcome
     * @return
     */
    public PlanItemAssertion assertRequired(boolean expectedOutcome) {
        boolean planItemRequired = getLast(RequiredRuleEvaluated.class).isRequired();
        if (planItemRequired != expectedOutcome) {
            if (planItemRequired) {
                throw new AssertionError(description + " is not expected to be required, but it is");
            } else {
                throw new AssertionError(description + " is expected to be required, but it is not");
            }
        }
        return this;
    }

    <T extends PlanItemEvent> T getLast(Class<T> tClass) {
        List<T> transitionEvents = new PublishedEventsAssertion(events).filter(tClass).getEvents();
        if (transitionEvents.isEmpty()) {
            throw new AssertionError(description + " is not yet in a state (case: "+caseId);
        }
        return transitionEvents.get(0);
    }
}
