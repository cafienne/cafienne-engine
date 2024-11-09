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

package org.cafienne.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskAction;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

/**
 * Event that happened on a HumanTask
 */
@Manifest
public class HumanTaskActivated extends HumanTaskTransitioned {
    private final Instant createdOn;
    private final String createdBy;
    private final String performer;
    private final Value<?> taskModel;

    public HumanTaskActivated(HumanTask task, String performer, Value<?> taskModel) {
        super(task, TaskState.Unassigned, TaskState.Null, TaskAction.Create);
        this.createdOn = task.getCaseInstance().getTransactionTimestamp();
        this.createdBy = task.getCaseInstance().getCurrentUser().id();
        this.performer = performer;
        this.taskModel = taskModel;
    }

    public HumanTaskActivated(ValueMap json) {
        super(json);
        this.createdOn = json.readInstant(Fields.createdOn);
        this.createdBy = json.readString(Fields.createdBy);
        this.performer = json.readString(Fields.performer);
        this.taskModel = json.get(Fields.taskModel);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTransitionEvent(generator);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.createdBy, createdBy);
        writeField(generator, Fields.performer, performer);
        writeField(generator, Fields.taskModel, taskModel);
    }
    public Instant getCreatedOn() {
        return createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Returns the (possibly null) name of the role that is to perform this task.
     *
     * @return
     */
    public String getPerformer() {
        return performer;
    }

    /**
     * Get the task-model / json schema for task
     * @return task-model / json schema for task
     */
    public String getTaskModel() {
        return taskModel.toString();
    }

    @Override
    public String toString() {
        return "HumanTask[" + getTaskId() + "] is active";
    }
}
