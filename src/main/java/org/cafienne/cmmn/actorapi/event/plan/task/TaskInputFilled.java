/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

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