/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.PlanItemEvent;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

@Manifest
public class PlanItemTransitioned extends PlanItemEvent {
    private final transient PlanItem planItem;

    private final State currentState;
    private final State historyState;
    private final Transition transition;

    public enum Fields {
        currentState, historyState, transition
    }

    public PlanItemTransitioned(PlanItem planItem) {
        super(planItem);
        this.planItem = planItem;
        this.currentState = planItem.getState();
        this.historyState = planItem.getHistoryState();
        this.transition = planItem.getLastTransition();
    }

    public PlanItemTransitioned(ValueMap json) {
        super(json);
        this.planItem = null;
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

    public Transition getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        String name = planItem != null ? planItem.getName() : "PlanItem";
        return name + "[" + getPlanItemId() + "]." + getTransition() + ", causing transition from " + getHistoryState() + " to " + getCurrentState();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writePlanItemEvent(generator);
        writeField(generator, Fields.currentState, currentState);
        writeField(generator, Fields.historyState, historyState);
        writeField(generator, Fields.transition, transition);
    }

    @Override
    protected void recoverPlanItemEvent(PlanItem planItem) {
        planItem.recover(this);
    }
}
