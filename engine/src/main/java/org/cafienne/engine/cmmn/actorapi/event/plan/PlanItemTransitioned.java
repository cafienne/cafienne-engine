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

package org.cafienne.engine.cmmn.actorapi.event.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.engine.cmmn.instance.State;
import org.cafienne.engine.cmmn.instance.Transition;
import org.cafienne.engine.cmmn.instance.sentry.StandardEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class PlanItemTransitioned extends CasePlanEvent implements StandardEvent<Transition, PlanItem<?>> {
    private final State currentState;
    private final State historyState;
    private final Transition transition;

    public PlanItemTransitioned(PlanItem<?> planItem, State newState, State historyState, Transition transition) {
        super(planItem);
        this.currentState = newState;
        this.historyState = historyState;
        this.transition = transition;
    }

    public PlanItemTransitioned(ValueMap json) {
        super(json);
        this.currentState = json.readEnum(Fields.currentState, State.class);
        this.historyState = json.readEnum(Fields.historyState, State.class);
        this.transition = json.readEnum(Fields.transition, Transition.class);
    }

    @Override
    public PlanItem<?> getSource() {
        return getPlanItem();
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
    public boolean hasBehavior() {
        return true;
    }

    @Override
    public void runImmediateBehavior() {
        if (getPlanItem().getCaseInstance().recoveryFinished()) {
            getPlanItem().runStateMachineAction(this);
            getPlanItem().informConnectedEntryCriteria(this);
        } else {
            // In recovery only update the connected entry criteria
            getPlanItem().informConnectedEntryCriteria(this);
        }
    }

    @Override
    public void runDelayedBehavior() {
        if (getPlanItem().getCaseInstance().recoveryFinished()) {
            getPlanItem().informParent(this);
            getPlanItem().informConnectedExitCriteria(this);
        } else {
            // In recovery only update the connected exit criteria
            getPlanItem().informConnectedExitCriteria(this);
        }
    }

    public Transition getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "[" + getName() + "/" + getPlanItemId() + "]: " + getHistoryState() + "." + getTransition().toString().toLowerCase() + "() ===> " + getCurrentState();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeCasePlanEvent(generator);
        writeField(generator, Fields.historyState, historyState);
        writeField(generator, Fields.transition, transition);
        writeField(generator, Fields.currentState, currentState);
    }

    @Override
    protected void updatePlanItemState(PlanItem<?> planItem) {
        planItem.publishTransition(this);
    }
}
