/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.actorapi.event.HumanTaskEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class HumanTaskMigrated extends HumanTaskEvent {
    private final String performer;
    private final Value<?> taskModel;

    public HumanTaskMigrated(HumanTask task, String performer, Value<?> taskModel) {
        super(task);
        this.performer = performer;
        this.taskModel = taskModel;
    }

    public HumanTaskMigrated(ValueMap json) {
        super(json);
        this.performer = json.readString(Fields.performer);
        this.taskModel = json.get(Fields.taskModel);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
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
    public String getTaskModel() {
        return taskModel.toString();
    }
}
