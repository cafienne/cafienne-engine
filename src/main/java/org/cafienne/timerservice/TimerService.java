package org.cafienne.timerservice;

import akka.persistence.SnapshotOffer;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        logger.error("Timer Service no longer supports snapshot offers. This functionality was deprecated in Cafienne Engine version 1.1.13 and is completely removed in version 1.1.18");
    }
}