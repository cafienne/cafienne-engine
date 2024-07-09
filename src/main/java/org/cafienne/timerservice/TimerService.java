/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.timerservice;

import org.apache.pekko.persistence.SnapshotOffer;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.system.CaseSystem;
import org.cafienne.timerservice.persistence.TimerStore;
import org.cafienne.timerservice.persistence.TimerStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TimerService extends ModelActor {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    public static final String CAFIENNE_TIMER_SERVICE = "cafienne-timer-service";
    final TimerStore storage;
    final TimerEventSink eventSink;
    final TimerMonitor monitor;

    public TimerService(CaseSystem caseSystem) {
        super(caseSystem);
        this.storage = new TimerStoreProvider(caseSystem).store();
        this.monitor = new TimerMonitor(this);
        this.eventSink = new TimerEventSink(this);
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
        logger.warn("Starting Timer Service - loading timers every " + Cafienne.config().engine().timerService().interval() + " for a window of " + Cafienne.config().engine().timerService().window() + " ahead");
        monitor.start();
        eventSink.start();
    }

    @Override
    protected void handleSnapshot(SnapshotOffer snapshot) {
        logger.error("Timer Service no longer supports snapshot offers. This functionality was deprecated in Cafienne Engine version 1.1.13 and is completely removed in version 1.1.18");
    }
}