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

package com.casefabric.cmmn.test.assertions;

import com.casefabric.cmmn.actorapi.event.plan.*;
import com.casefabric.cmmn.instance.PlanItem;
import com.casefabric.cmmn.instance.PlanItemType;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.test.CaseTestCommand;

import java.util.Collections;
import java.util.List;

/**
 * Some basic assertions for plan items
 */
public class PlanItemAssertion extends ModelTestCommandAssertion {
    protected final String caseId;

    private final String id;
    private final String name;
    private final PlanItemType type;
    private final String description;
    private final List<CasePlanEvent> events;

    PlanItemAssertion(CaseTestCommand command, PlanItemCreated pic) {
        super(command);
        this.caseId = command.getCaseInstanceId();
        this.id = pic.getPlanItemId();
        this.name = pic.getPlanItemName();
        this.type = pic.getType();
        this.description = type + " '" + name + "' with id "+id;

        PublishedEventsAssertion<CasePlanEvent> allCasePlanEvents = command.getEventListener().getEvents().filter(CasePlanEvent.class);
        this.events = allCasePlanEvents.filter(pie -> pie.getPlanItemId().equals(this.id)).getEvents();
        Collections.reverse(this.events);// Reverse order the events, such that last one comes first.
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    protected PlanItemAssertion assertType(PlanItemType ...expectedTypes) {
        for (PlanItemType expectedType : expectedTypes) {
            if (this.type.equals(expectedType)) {
                return this;
            }
        }
        throw new AssertionError("Plan item " + name + " is of type " + type + ", but that is not as expected");
    }

    /**
     * Asserts that the plan item's instance has the expected type (e.g., Task, Stage, Milestone)
     */
    public <T extends PlanItem<?>> PlanItemAssertion assertType(PlanItemType expectedType) {
        if (!type.equals(expectedType)) {
            throw new AssertionError("Plan item " + name + " is of type " + type + ", but is expected to be of type " + expectedType);
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

    <T extends CasePlanEvent> T getLast(Class<T> tClass) {
        List<T> transitionEvents = new PublishedEventsAssertion(events).filter(tClass).getEvents();
        if (transitionEvents.isEmpty()) {
            throw new AssertionError(description + " is not yet in a state (case: "+caseId);
        }
        return transitionEvents.get(0);
    }
}
