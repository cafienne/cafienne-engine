/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.plan.eventlistener.TimerSet;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.TimerEventDefinition;
import org.cafienne.timerservice.akka.command.CancelTimer;
import org.cafienne.timerservice.akka.command.SetTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class TimerEvent extends PlanItem<TimerEventDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(TimerEvent.class);

    public TimerEvent(String id, int index, ItemDefinition itemDefinition, TimerEventDefinition definition, Stage stage) {
        super(id, index, itemDefinition, definition, stage, StateMachine.EventMilestone);
    }

    private Instant targetMoment = null;

    public void updateState(TimerSet event) {
        this.targetMoment = event.getTargetMoment();
        setSchedule();
    }

    private void setSchedule() {
        getCaseInstance().askTimerService(new SetTimer(getCaseInstance().getCurrentUser(), this, targetMoment), left -> {
            logger.error("Failed to set timer", left.exception());
            addDebugInfo(() -> "Failed to set timer", left.toJson());
            makeTransition(Transition.Fault);
        });
    }

    private void removeSchedule() {
        getCaseInstance().askTimerService(new CancelTimer(getCaseInstance().getCurrentUser(), this), left -> {
            logger.error("Failed to cancel timer", left.exception());
            addDebugInfo(() -> "Failed to cancel timer", left.toJson());
            makeTransition(Transition.Fault);
        });
    }

    @Override
    protected void createInstance() {
        addEvent(new TimerSet(this));
    }

    @Override
    protected void completeInstance() {
    }

    @Override
    protected void suspendInstance() {
        // Suspending is done by simply removing the schedule. If we are resumed, we simply create a new schedule.
        removeSchedule();
    }

    @Override
    protected void resumeInstance() {
        // Set a new schedule to make our plan item "Occur"
        setSchedule();
    }

    @Override
    protected void terminateInstance() {
        // Remove the schedule as it has become useless (if it still would go off, nothing would happen, since "Occur" has no transition once Terminated)
        removeSchedule();
    }
}
