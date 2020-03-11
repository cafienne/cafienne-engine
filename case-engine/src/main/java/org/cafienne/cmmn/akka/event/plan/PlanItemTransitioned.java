/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

@Manifest
public class PlanItemTransitioned extends PlanItemEvent {
    private final State currentState;
    private final State historyState;
    private final Transition transition;

    public enum Fields {
        currentState, historyState, transition
    }

    public PlanItemTransitioned(PlanItem planItem, State newState, State historyState, Transition transition) {
        super(planItem);
        this.currentState = newState;
        this.historyState = historyState;
        this.transition = transition;
    }

    public PlanItemTransitioned(ValueMap json) {
        super(json);
        this.currentState = json.getEnum(Fields.currentState, State.class);
        this.historyState = json.getEnum(Fields.historyState, State.class);
        this.transition = json.getEnum(Fields.transition, Transition.class);
    }

    public State getCurrentState() {
        return currentState;
    }

    public State getHistoryState() {
        if (historyState == null) {
            return State.Null;
        }
        return historyState;
    }

    @Override
    public void runBehavior() {
        planItem.runBehavior(this);
    }

    public Transition getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + getName() + "/" + getPlanItemId() + "]: " + getHistoryState() + "." + getTransition().toString().toLowerCase() + "() ===> " + getCurrentState();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writePlanItemEvent(generator);
        writeField(generator, Fields.historyState, historyState);
        writeField(generator, Fields.transition, transition);
        writeField(generator, Fields.currentState, currentState);
    }

    @Override
    protected void updatePlanItemState(PlanItem planItem) {
        planItem.updateState(this);
    }
}
