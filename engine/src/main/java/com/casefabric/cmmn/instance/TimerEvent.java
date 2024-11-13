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

package com.casefabric.cmmn.instance;

import com.casefabric.cmmn.actorapi.event.plan.eventlistener.*;
import com.casefabric.cmmn.definition.ItemDefinition;
import com.casefabric.cmmn.definition.TimerEventDefinition;
import com.casefabric.humantask.actorapi.event.migration.HumanTaskDropped;

import java.time.Instant;

public class TimerEvent extends EventListener<TimerEventDefinition> {
    public TimerEvent(String id, int index, ItemDefinition itemDefinition, TimerEventDefinition definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage);
    }

    private Instant targetMoment = null;

    public void updateState(TimerSet event) {
        this.targetMoment = event.getTargetMoment();
        addDebugInfo(() -> super.toString() + " occurs at " + targetMoment);
    }

    @Override
    protected void createInstance() {
        addEvent(new TimerSet(this));
    }

    @Override
    protected void completeInstance() {
        addEvent(new TimerCompleted(this));
    }

    @Override
    protected void suspendInstance() {
        addEvent(new TimerSuspended(this));
    }

    @Override
    protected void resumeInstance() {
        addEvent(new TimerResumed(this));
    }

    @Override
    protected void terminateInstance() {
        addEvent(new TimerTerminated(this));
    }

    @Override
    protected void lostDefinition() {
        super.lostDefinition();
        addEvent(new TimerDropped(this));
    }
}
