/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.actorapi.event.plan.eventlistener.*;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.TimerEventDefinition;

import java.time.Instant;

public class TimerEvent extends PlanItem<TimerEventDefinition> {
    public TimerEvent(String id, int index, ItemDefinition itemDefinition, TimerEventDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage, StateMachine.EventMilestone);
    }

    private Instant targetMoment = null;

    public void updateState(TimerSet event) {
        this.targetMoment = event.getTargetMoment();
        addDebugInfo(() -> super.toString() + " occurs at " + targetMoment);
    }

    @Override
    protected void createInstance() {
        addEvent(new TimerSet(this));
    }

    @Override
    protected void completeInstance() {
        addEvent(new TimerCompleted(this));
    }

    @Override
    protected void suspendInstance() {
        addEvent(new TimerSuspended(this));
    }

    @Override
    protected void resumeInstance() {
        addEvent(new TimerResumed(this));
    }

    @Override
    protected void terminateInstance() {
        addEvent(new TimerTerminated(this));
    }
}
