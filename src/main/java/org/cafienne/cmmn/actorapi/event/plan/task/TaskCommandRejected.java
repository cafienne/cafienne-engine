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
