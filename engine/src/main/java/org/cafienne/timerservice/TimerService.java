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

import org.apache.pekko.actor.AbstractActor;
import org.cafienne.system.CaseSystem;
import org.cafienne.timerservice.persistence.TimerStore;
import org.cafienne.timerservice.persistence.TimerStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TenantActor manages users and their roles inside a tenant.
 */
public class TimerService extends AbstractActor {
    private final static Logger logger = LoggerFactory.getLogger(TimerService.class);
    public static final String IDENTIFIER = "case-engine-timer-service";
    final CaseSystem caseSystem;
    final TimerStore storage;
    final TimerEventSink eventSink;
    final TimerMonitor monitor;

    public TimerService(CaseSystem caseSystem) {
        this.caseSystem = caseSystem;
        this.storage = new TimerStoreProvider(caseSystem).store();
        this.monitor = new TimerMonitor(this);
        this.eventSink = new TimerEventSink(this);
        logger.warn("Starting Timer Service - loading timers every {} for a window of {} ahead", caseSystem.config().engine().timerService().interval(), caseSystem.config().engine().timerService().window());
        monitor.start();
        eventSink.start();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(Object.class, monitor::handleActorMessage).build();
    }
}