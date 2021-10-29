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
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class HumanTaskAssigned extends HumanTaskTransitioned {
    /**
     * New assignee of the task
     */
    public final String assignee; // assignee of the task

    public HumanTaskAssigned(HumanTask task, String assignee) {
        this(task, assignee, TaskState.Assigned, TaskAction.Assign);
    }

    protected HumanTaskAssigned(HumanTask task, String assignee, TaskState nextState, TaskAction transition) {
        super(task, nextState, transition);
        this.assignee = assignee;
    }

    public HumanTaskAssigned(ValueMap json) {
        super(json);
        this.assignee = json.readString(Fields.assignee);
    }

    @Override
    public void updateState(WorkflowTask task) {
        super.updateState(task);
        task.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTransitionEvent(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
