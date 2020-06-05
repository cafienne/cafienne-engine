package org.cafienne.timerservice;

import akka.actor.Cancellable;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SnapshotOffer;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.handler.AkkaSystemMessageHandler;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.timerservice.akka.command.CancelTimer;
import org.cafienne.timerservice.akka.command.SetTimer;
import org.cafienne.timerservice.akka.command.TimerServiceCommand;
import org.cafienne.timerservice.akka.command.response.TimerServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TimerService extends ModelActor<TimerServiceCommand, ModelEvent> {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    public static final String CAFIENNE_TIMER_SERVICE = "cafienne-timer-service";

    private TimerStorage storage = new TimerStorage();
    private Map<String, ScheduledTimer> timers = new HashMap();

    public TimerService() {
        super(TimerServiceCommand.class, ModelEvent.class);
        setEngineVersion(CaseSystem.version());
        addPeriodicSnapshotSaver();
    }

    @Override
    public String persistenceId() {
        return CAFIENNE_TIMER_SERVICE;
    }

    @Override
    protected AkkaSystemMessageHandler createAkkaSystemMessageHandler(Object message) {
        // Typically invoked upon succesful snapshot saving.

        if (message instanceof SaveSnapshotFailure) {
            SaveSnapshotFailure failure = (SaveSnapshotFailure) message;
            // How to go about this?
            logger.error("TIMER SERVICE ERROR: Could not save snapshot for timer service", failure.cause());
        }
        return super.createAkkaSystemMessageHandler(message);
    }

    @Override
    protected CommandHandler createCommandHandler(TimerServiceCommand command) {
        return new TimerCommandHandler(this, command);
    }

    @Override
    public String getParentActorId() {
        return "";
    }

    @Override
    public String getRootActorId() {
        return getId();
    }

    @Override
    public ModelEvent createLastModifiedEvent(Instant lastModified) {
        return null;
    }

    @Override
    protected void enableSelfCleaner() {
        // Make sure TimerService remains in memory and is not removed each and every time
    }

    private void setTimer(TimerJob job) {
        timers.put(job.timerId, new ScheduledTimer(job));
    }

    private void removeTimer(String timerId) {
        timers.get(timerId).cancel();
        storage.removeTimer(timerId);
        storageChanged = true;
    }

    public TimerServiceResponse handle(SetTimer command) {
        TimerJob job = new TimerJob(command);
        setTimer(job);
        storage.addTimer(job);
        saveSnapshot(storage);

        return new TimerServiceResponse(command);
    }

    public TimerServiceResponse handle(CancelTimer command) {
        removeTimer(command.timerId);
        return new TimerServiceResponse(command);
    }

    private void refreshStorageObject(TimerStorage storage) {
        this.storage = storage;
        storage.getTimers().forEach(this::setTimer);
    }

    @Override
    protected void handleRecovery(Object event) {
        if (event instanceof SnapshotOffer) {
            SnapshotOffer offer = (SnapshotOffer) event;
            Object snapshot = offer.snapshot();
            if (snapshot instanceof TimerStorage) {
                refreshStorageObject((TimerStorage) snapshot);
            }
        } else {
            super.handleRecovery(event);
        }
    }

    private boolean storageChanged = false;

    /**
     * Adds a thread that will check every now and then whether the timers have changed, and if so stores the snapshot.
     */
    void addPeriodicSnapshotSaver() {
        Runnable saveJob = () -> {
            try {
                while (true) {
                    FiniteDuration duration = Duration.create(10, TimeUnit.SECONDS);
                    Thread.sleep(duration.toMillis());
                    if (logger.isDebugEnabled()) {
                        storage.getTimers().forEach(job -> {
                            long waitAnother = job.moment.toEpochMilli() - Instant.now().toEpochMilli();
                            logger.debug("Job[" + job.timerId + "] scheduled at " + job.moment +" (" + (waitAnother/1000) +" seconds)");
                        });
                    }
                    if (storageChanged) {
                        saveSnapshot(storage);
                        storageChanged = false;
                    } else {
                        logger.debug("Storage did not change; checking again in " + duration);
                    }
                }
            } catch (InterruptedException e) {
                // Got interrupted. return
            }
        };
        new Thread(saveJob).start();
    }

    class ScheduledTimer {
        final TimerJob event;
        final Cancellable schedule;

        ScheduledTimer(TimerJob event) {
            this.event = event;
            MakePlanItemTransition command = new MakePlanItemTransition(event.user, event.caseInstanceId, event.timerId, Transition.Occur, "");

            // Note: this code needs some refactoring
            // - We should not use the system scheduler (dispatcher), but create our own (?)
            // - We should leverage more of java.time (i.e., not rely on System.currentTimeMillis, but get a java.time.Duration instead)
            long millis = event.moment.toEpochMilli();
            long delay = millis - System.currentTimeMillis();

            FiniteDuration duration = Duration.create(delay, TimeUnit.MILLISECONDS);
//            System.out.println("Scheduling to run timer event " + event.timerId + " in " + (duration.length() / 1000) + " seconds from now");
            Runnable job = () -> {
//                System.out.println("Raising timer event " + event.timerId);
//                addDebugInfo(() -> "Raising timer event " + event.caseInstanceId + "/" + event.timerId);
                askCase(command, failure -> {
                    // Is logging an error sufficient? Or should we go Fault?!
//                    System.out.println("Called back failure");
                    logger.error("Could not make event " + getId() + " occur\n" + failure);
                }, success -> {
//                    System.out.println("Called back ok");
                });
//                System.out.println("Removing timer");
                removeTimer(event.timerId);
            };

            schedule = getScheduler().schedule(duration, job);
        }

        void cancel() {
            schedule.cancel();
        }
    }
}