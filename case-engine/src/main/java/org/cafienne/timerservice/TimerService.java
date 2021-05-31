package org.cafienne.timerservice;

import akka.persistence.SnapshotOffer;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.config.Cafienne;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
@Deprecated
public class TimerService extends ModelActor<ModelCommand, ModelEvent> {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    public static final String CAFIENNE_TIMER_SERVICE = "cafienne-timer-service";

    public TimerService(CaseSystem caseSystem) {
        super(ModelCommand.class, ModelEvent.class, caseSystem);
        setEngineVersion(Cafienne.version());
    }

    @Override
    public String persistenceId() {
        return CAFIENNE_TIMER_SERVICE;
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

    @Override
    protected boolean inNeedOfTenantInformation() {
        // No need of tenant information, as this is a singleton actor in this JVM that is tenant-agnostic
        return false;
    }

    @Override
    protected void recoveryCompleted() {
        logger.info("Timer service recovered, reading current timers from database");
        // TODO: start listening to event stream
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
            logger.info("Bumped into a snapshot with " + existingTimers.size() + " timers; conversion is done implicitly in the new CafienneTimerService");
        }
    }
}