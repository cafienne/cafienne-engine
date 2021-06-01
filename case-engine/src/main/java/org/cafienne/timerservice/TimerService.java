package org.cafienne.timerservice;

import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SnapshotOffer;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.config.Cafienne;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.handler.AkkaSystemMessageHandler;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.timerservice.akka.command.CancelTimer;
import org.cafienne.timerservice.akka.command.SetTimer;
import org.cafienne.timerservice.akka.command.TimerServiceCommand;
import org.cafienne.timerservice.akka.command.response.TimerServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

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
    public TransactionEvent createTransactionEvent() {
        return null;
    }

    @Override
    protected void enableSelfCleaner() {
        // Make sure TimerService remains in memory and is not removed each and every time
    }

    private void setTimer(TimerJob job) {
        timers.put(job.timerId, new ScheduledTimer(this, job));
    }

    void removeTimer(String timerId, boolean externalRequest) {
        ScheduledTimer timer = timers.get(timerId);
        if (externalRequest) {
            logger.debug("Canceling timer " + timerId +" from case " + timer.request.caseInstanceId);
        }
        storage.removeTimer(timerId);
        if (timer != null) {
            timer.cancel();
        } else {
            logger.debug("Timer " + timerId + " is removed, but was not found in the schedule");
        }
    }

    public TimerServiceResponse handle(SetTimer command) {
        TimerJob job = new TimerJob(command);
        setTimer(job);
        storage.addTimer(job);
        saveTimerStorage("because new timer is set");

        return new TimerServiceResponse(command);
    }

    public TimerServiceResponse handle(CancelTimer command) {
        removeTimer(command.timerId, true);
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

    void saveTimerStorage(String msg) {
        if (storage.changed()) {
            logger.debug("Storage changed, saving snapshot " + msg);
            saveSnapshot(storage);
            storage.saved();
        } else {
            logger.debug("Store was not changed and will not be saved " + msg);
        }
    }

    /**
     * Adds a thread that will check every now and then whether the timers have changed, and if so stores the snapshot.
     */
    void addPeriodicSnapshotSaver() {
        Runnable saveJob = () -> {
            try {
                while (true) {
                    FiniteDuration duration = Duration.create(CaseSystem.config().engine().timerService().persistDelay(), TimeUnit.SECONDS);
                    Thread.sleep(duration.toMillis());
                    saveTimerStorage("after period of " + duration);
//                    if (logger.isDebugEnabled())
//                    {
//                        storage.getTimers().forEach(job -> {
//                            long waitAnother = job.moment.toEpochMilli() - Instant.now().toEpochMilli();
//                            logger.warn("Job[" + job.timerId + "] scheduled at " + job.moment +" (" + (waitAnother/1000) +" seconds)");
//                        });
//                    }
//                    if (storageChanged) {
//                    } else {
//                        logger.warn("Storage did not change; checking again in " + duration);
//                    }
                }
            } catch (InterruptedException e) {
                // Got interrupted. return
                logger.debug("Received an interrupt; returning");
            }
        };
        new Thread(saveJob).start();
    }

    @Override
    protected boolean inNeedOfTenantInformation() {
        // No need of tenant information, as this is a singleton actor in this JVM that is tenant-agnostic
        return false;
    }

}