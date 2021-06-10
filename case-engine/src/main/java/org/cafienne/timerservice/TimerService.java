package org.cafienne.timerservice;

import akka.persistence.SnapshotOffer;
import org.cafienne.system.CaseSystem;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TimerService extends ModelActor<ModelCommand, ModelEvent> {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    public static final String CAFIENNE_TIMER_SERVICE = "cafienne-timer-service";
    private final TimerEventSink timerstream;

    public TimerService(CaseSystem caseSystem) {
        super(ModelCommand.class, ModelEvent.class, caseSystem);
        this.timerstream = new TimerEventSink(this, caseSystem, caseSystem.system());
        setEngineVersion(Cafienne.version());
    }

    @Override
    public String persistenceId() {
        return CAFIENNE_TIMER_SERVICE;
    }

    @Override
    public TransactionEvent createTransactionEvent() {
        return null;
    }

    @Override
    protected void enableSelfCleaner() {
        // Make sure TimerService remains in memory and is not removed each and every time
    }

    @Override
    protected boolean inNeedOfTenantInformation() {
        // No need of tenant information, as this is a singleton actor in this JVM that is tenant-agnostic
        return false;
    }

    @Override
    protected void recoveryCompleted() {
        logger.info("Starting Timer Service");
        timerstream.open();
    }

    @Override
    protected void handleRecovery(Object object) {
        // Handle migration of old style of timer persistence
        if (object instanceof SnapshotOffer) {
            migrateSnapshot((SnapshotOffer) object);
        } else {
            super.handleRecovery(object);
        }
    }

    private void migrateSnapshot(SnapshotOffer offer) {
        Object snapshot = offer.snapshot();
        if (snapshot instanceof TimerStorage) {
            Collection<TimerJob> existingTimers = ((TimerStorage) snapshot).getTimers();
            if (!existingTimers.isEmpty()) {
                logger.info("Found an existing snapshot with " + existingTimers.size() + " timers; migrating them to the new storage");
                List<Timer> legacy = existingTimers.stream().map(job -> new Timer(job.caseInstanceId, job.timerId, job.moment, job.user)).collect(Collectors.toList());
                timerstream.migrateTimers(legacy);
                logger.info("Successfully migrated timers to the new storage; clearing snapshot");
                saveSnapshot(new TimerStorage());
            }
        }
    }
}