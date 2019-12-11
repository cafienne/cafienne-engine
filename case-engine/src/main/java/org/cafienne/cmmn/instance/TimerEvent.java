/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import akka.actor.Cancellable;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.event.eventlistener.TimerSet;
import org.cafienne.cmmn.definition.TimerEventDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class TimerEvent extends PlanItemDefinitionInstance<TimerEventDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(TimerEvent.class);

    public TimerEvent(PlanItem planItem, TimerEventDefinition definition) {
        super(planItem, definition, StateMachine.EventMilestone);
    }

    private Cancellable schedule;
    private Instant targetMoment =  null;

    private Cancellable scheduleWork(Instant moment, MakePlanItemTransition command) {
        // Note: this code needs some refactoring
        // - We should not use the system scheduler (dispatcher), but create our own (?)
        // - We should leverage more of java.time (i.e., not rely on System.currentTimeMillis, but get a java.time.Duration instead)
        long millis = moment.toEpochMilli();
        long delay = millis - System.currentTimeMillis();

        FiniteDuration duration = Duration.create(delay, TimeUnit.MILLISECONDS);
        Runnable job = () -> {
            addDebugInfo(() -> "Raising timer event " + getPlanItem().getName() + "/" + getPlanItem().getId());
            getCaseInstance().askCase(command, failure -> // Is logging an error sufficient? Or should we go Fault?!
                logger.error("Could not make event "+getPlanItem().getId()+" occur\n" + failure));
        };
        
        Cancellable schedule = getCaseInstance().getScheduler().schedule(duration, job);
        return schedule;
    }

    private void evaluateTargetMoment() {
        // Plan/resume the timer ...
        this.targetMoment = this.getDefinition().getMoment(this);
        getCaseInstance().storeInternallyGeneratedEvent(new TimerSet(this)).finished();
    }

    public void recover(TimerSet event) {
        this.targetMoment = event.getTargetMoment();
        if (schedule == null) {
            setSchedule();
        }
    }

    public Instant getTargetMoment() {
        if (targetMoment == null) {
            evaluateTargetMoment();
        }
        return targetMoment;
    }

    private void setSchedule() {
//        System.out.println("\n\n\n setting schedule\n\n");
        MakePlanItemTransition command = new MakePlanItemTransition(getCaseInstance().getCurrentUser(), getCaseInstance().getId(), this.getId(), Transition.Occur, this.getPlanItem().getName());
        schedule = scheduleWork(getTargetMoment(), command);
    }

    private void removeSchedule() {
        if (schedule != null) {
            schedule.cancel();
        }
    }

    @Override
    protected void createInstance() {
        // Upon creation we will schedule a timer to make our plan item "Occur" at the specified moment.
        setSchedule();
    }

    @Override
    protected void completeInstance() {
        removeSchedule();
    }

    @Override
    protected void suspendInstance() {
//        System.out.println("SUSPENDING SCHEDULE");
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
