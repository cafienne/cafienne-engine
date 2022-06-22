/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class TaskCommandRejected extends TaskEvent<Task<?>> {
    private final ValueMap command;
    private final ValueMap failure;

    public TaskCommandRejected(Task<?> task, ModelCommand command, ValueMap failure) {
        super(task);
        this.command = command.rawJson();
        this.failure = failure;
    }

    public TaskCommandRejected(ValueMap json) {
        super(json);
        this.command = json.readMap(Fields.command);
        this.failure = json.readMap(Fields.failure);
    }

    @Override
    public void updateState(Task<?> task) {
        // Just for logging purposes, no actual state change
        task.updateState(this);
        task.goFault(new ValueMap("exception", failure));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
        writeField(generator, Fields.command, command);
        writeField(generator, Fields.failure, failure);
    }

    @Override
    public String toString() {
        return "Task " + getTaskId() + " has output:\n" + command;
    }
}
