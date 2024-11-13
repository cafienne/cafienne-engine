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

package com.casefabric.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.instance.Task;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class TaskOutputFilled extends TaskEvent<Task<?>> {
    private final ValueMap parameters;
    private final ValueMap rawOutputParameters;

    public TaskOutputFilled(Task<?> task, ValueMap outputParameters, ValueMap rawOutputParameters) {
        super(task);
        this.parameters = outputParameters;
        this.rawOutputParameters = rawOutputParameters;
    }

    public TaskOutputFilled(ValueMap json) {
        super(json);
        this.parameters = json.readMap(Fields.parameters);
        this.rawOutputParameters = json.readMap(Fields.rawOutputParameters);
    }

    @Override
    public void updateState(Task<?> task) {
        task.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
        writeField(generator, Fields.parameters, parameters);
        writeField(generator, Fields.rawOutputParameters, rawOutputParameters);
    }

    @Override
    public String toString() {
        return "Task " + getTaskId() + " has output:\n" + parameters;
    }

    public ValueMap getTaskOutputParameters() {
        return parameters;
    }

    public ValueMap getRawOutputParameters() {
        return rawOutputParameters;
    }

}
