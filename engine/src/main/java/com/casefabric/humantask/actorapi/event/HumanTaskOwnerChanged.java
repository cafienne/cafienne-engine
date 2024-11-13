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
import com.casefabric.humantask.instance.WorkflowTask;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class HumanTaskOwnerChanged extends HumanTaskEvent {
    /**
     * New owner of the task
     */
    public final String owner; // new owner of the task


    public HumanTaskOwnerChanged(HumanTask task, String owner) {
        super(task);
        this.owner = owner;
    }

    public HumanTaskOwnerChanged(ValueMap json) {
        super(json);
        this.owner = json.readString(Fields.owner);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.owner, owner);
    }

    @Override
    public void updateState(WorkflowTask task) {
        super.updateState(task);
        task.updateState(this);
    }
}
