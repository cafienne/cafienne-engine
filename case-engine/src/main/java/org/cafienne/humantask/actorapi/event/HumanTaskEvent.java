/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskEvent;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class HumanTaskEvent extends TaskEvent<HumanTask> {
    public static final String TAG = "cafienne:task";

    public final String taskId; // taskName is same as the planItem id
    private final String taskName; // taskName is same as the planItemName

    /**
     * Constructor used by HumanTaskCreated event, since at that moment the task name is not yet known
     * inside the task actor.
     * @param task
     */
    protected HumanTaskEvent(HumanTask task) {
        super(task);
        this.taskName = task.getName();
        this.taskId = task.getId();
    }

    protected HumanTaskEvent(ValueMap json) {
        super(json);
        this.taskName = readField(json, Fields.taskName);
        this.taskId = readField(json, Fields.taskId);
    }

    protected void writeHumanTaskEvent(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
        writeField(generator, Fields.taskName, taskName);
        writeField(generator, Fields.taskId, taskId);
    }

    @Override
    public final void updateState(HumanTask task) {
        updateState(task.getImplementation());
    }

    protected void updateState(WorkflowTask task) {
    }

    @Override
    public String getPlanItemId() {
        // Unfortunately need to override this, because recovery uses the plan item id,
        // and older versions of HumanTaskEvent did not invoke parent's serializer.
        return taskId;
    }

    /**
     * Get the name of the task
     * @return
     */
    public String getTaskName() {
        return taskName;
    }
}
