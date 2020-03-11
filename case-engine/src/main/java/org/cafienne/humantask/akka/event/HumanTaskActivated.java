/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskAction;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;

/**
 * Event that happened on a HumanTask
 */
@Manifest
public class HumanTaskActivated extends HumanTaskTransitioned {
    private final String performer;
    private final ValueMap taskModel;

    private enum Fields {
        performer, taskModel
    }

    public HumanTaskActivated(HumanTask task, String performer, ValueMap taskModel) {
        super(task, TaskState.Unassigned, TaskState.Null, TaskAction.Create);
        this.performer = performer;
        this.taskModel = taskModel;
    }

    public HumanTaskActivated(ValueMap json) {
        super(json);
        this.performer = json.raw(Fields.performer);
        this.taskModel = readMap(json, Fields.taskModel);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTransitionEvent(generator);
        writeField(generator, Fields.performer, performer);
        writeField(generator, Fields.taskModel, taskModel);
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
    public ValueMap getTaskModel() {
        return taskModel;
    }

    @Override
    public String toString() {
        return "HumanTask[" + getTaskId() + "] is active";
    }
}
