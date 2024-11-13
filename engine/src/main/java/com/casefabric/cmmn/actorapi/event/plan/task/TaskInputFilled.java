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
public class TaskInputFilled extends TaskEvent<Task<?>> {
    private final ValueMap taskParameters;
    private final ValueMap mappedInputParameters;

    public TaskInputFilled(Task<?> task, ValueMap inputParameters, ValueMap mappedInputParameters) {
        super(task);
        this.taskParameters = inputParameters;
        this.mappedInputParameters = mappedInputParameters;
    }

    public TaskInputFilled(ValueMap json) {
        super(json);
        this.taskParameters = json.readMap(Fields.taskParameters);
        this.mappedInputParameters = json.readMap(Fields.mappedInputParameters);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
        writeField(generator, Fields.taskParameters, taskParameters);
        writeField(generator, Fields.mappedInputParameters, mappedInputParameters);
    }

    @Override
    public String toString() {
        return "Task[" + getTaskId() + "] has input:\n" + taskParameters;
    }

    public ValueMap getTaskInputParameters() {
        return taskParameters;
    }

    public ValueMap getMappedInputParameters() {
        return mappedInputParameters;
    }

    @Override
    public void updateState(Task<?> task) {
        task.updateState(this);
    }
}
