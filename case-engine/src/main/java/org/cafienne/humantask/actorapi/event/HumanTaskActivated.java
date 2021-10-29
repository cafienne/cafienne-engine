/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
