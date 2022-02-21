package org.cafienne.timerservice;

import akka.persistence.SnapshotOffer;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TimerService extends ModelActor {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    public static final String CAFIENNE_TIMER_SERVICE = "cafienne-timer-service";
    private final TimerEventSink timerstream;

    public TimerService(CaseSystem caseSystem) {
        super(caseSystem);
        this.timerstream = new TimerEventSink(this, caseSystem);
        setEngineVersion(Cafienne.version());
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return false;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return false;
    }

    @Override
    protected boolean hasAutoShutdown() {
        return false;
    }

    @Override
    public String persistenceId() {
        return CAFIENNE_TIMER_SERVICE;
    }

    @Override
    protected void recoveryCompleted() {
        logger.info("Starting Timer Service");
        timerstream.open();
    }

    @Override
    protected void handleSnapshot(SnapshotOffer snapshot) {
        // Handle migration of old style of timer persistence
        migrateSnapshot(snapshot);
    }

    private void migrateSnapshot(SnapshotOffer offer) {
        Object snapshot = offer.snapshot();
        if (snapshot instanceof TimerStorage) {
            Collection<TimerJob> existingTimers = ((TimerStorage) snapshot).getTimers();
            if (!existingTimers.isEmpty()) {
                logger.info("Found an existing snapshot with " + existingTimers.size() + " timers; migrating them to the new storage");
                List<Timer> legacy = existingTimers.stream().map(job -> new Timer(job.caseInstanceId, job.timerId, job.moment, job.user.id())).collect(Collectors.toList());
                timerstream.migrateTimers(legacy);
                logger.info("Successfully migrated timers to the new storage; clearing snapshot");
                saveSnapshot(new TimerStorage());
            }
        }
    }
}