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

package com.casefabric.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class HumanTaskCreated extends HumanTaskEvent {
    private final Instant createdOn;
    private final String createdBy;

    @Deprecated
    public HumanTaskCreated(HumanTask task) {
        super(task);
        this.createdOn = task.getCaseInstance().getTransactionTimestamp();
        this.createdBy = task.getCaseInstance().getCurrentUser().id();
        throw new IllegalArgumentException("This code is no longer in use");
    }

    public HumanTaskCreated(ValueMap json) {
        super(json);
        this.createdOn = json.readInstant(Fields.createdOn);
        this.createdBy = json.readString(Fields.createdBy);
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return "HumanTask[" + getTaskId() + "] is active";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.createdBy, createdBy);
    }
}
