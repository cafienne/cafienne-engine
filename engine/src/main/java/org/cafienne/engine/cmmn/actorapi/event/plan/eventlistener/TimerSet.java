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

package org.cafienne.engine.cmmn.actorapi.event.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.TimerEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class TimerSet extends TimerBaseEvent {
    private final static Logger logger = LoggerFactory.getLogger(TimerSet.class);

    public final String rootCaseId;
    private final Instant targetMoment;
    private transient TimerEvent timerEvent;

    public TimerSet(TimerEvent timerEvent) {
        super(timerEvent);
        this.rootCaseId = timerEvent.getCaseInstance().getRootCaseId();
        this.timerEvent = timerEvent;
        this.targetMoment = timerEvent.getDefinition().getMoment(timerEvent);
    }

    public TimerSet(ValueMap json) {
        super(json);
        this.targetMoment = json.readInstant(Fields.targetMoment);
        this.rootCaseId = json.readString(Fields.rootCaseId);
    }

    public Instant getTargetMoment() {
        return targetMoment;
    }

    @Override
    public void updateState(Case actor) {
        if (timerEvent == null) {
            timerEvent = actor.getPlanItemById(getTimerId());
            if (timerEvent == null) {
                logger.error("MAJOR ERROR: Cannot recover task timerEvent for task with id " + getTimerId() + ", because the plan item cannot be found");
                return;
            }
        }
        timerEvent.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTimerEvent(generator);
        writeField(generator, Fields.targetMoment, targetMoment);
        writeField(generator, Fields.rootCaseId, rootCaseId);
    }

    @Override
    public String toString() {
        return "Timer "+getTimerId()+" is set to occur at " + targetMoment;
    }
}
