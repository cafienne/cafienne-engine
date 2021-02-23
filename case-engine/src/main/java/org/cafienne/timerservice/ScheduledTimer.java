package org.cafienne.timerservice;

import akka.actor.Cancellable;
import org.cafienne.cmmn.akka.command.plan.eventlistener.RaiseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

class ScheduledTimer {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    private final TimerService timerService;
    final TimerJob request;
    final Cancellable schedule;

    ScheduledTimer(TimerService timerService, TimerJob request) {
        this.timerService = timerService;
        this.request = request;
        RaiseEvent command = new RaiseEvent(request.user, request.caseInstanceId, request.timerId);

        // Note: this code needs some refactoring
        // - We should not use the system scheduler (dispatcher), but create our own (?)
        // - We should leverage more of java.time (i.e., not rely on System.currentTimeMillis, but get a java.time.Duration instead)
        long millis = request.moment.toEpochMilli();
        long delay = millis - System.currentTimeMillis();

        FiniteDuration duration = Duration.create(delay, TimeUnit.MILLISECONDS);
        if (logger.isDebugEnabled()) {
//            System.out.println("Scheduling to run timer request " + request.timerId + " in " + (duration.length() / 1000) + " seconds from now");
            logger.debug("Scheduling to run timer request " + request.timerId + " in " + (duration.length() / 1000) + " seconds from now");
        }
        Runnable job = () -> {
            if (logger.isDebugEnabled()) {
                logger.debug("Raising timer in case " + request.caseInstanceId+" for timer " + request.timerId + " on behalf of user " + request.user.id());
            }
            timerService.askCase(command, failure -> {
                // Is logging an error sufficient? Or should we go Fault?!
                logger.error("Could not make event " + timerService.getId() + " occur\n" + failure);
            }, success -> {
//                    System.out.println("Called back ok");
            });
            timerService.removeTimer(request.timerId, false);
        };

        schedule = timerService.getScheduler().schedule(duration, job);
    }

    void cancel() {
        schedule.cancel();
    }
}
