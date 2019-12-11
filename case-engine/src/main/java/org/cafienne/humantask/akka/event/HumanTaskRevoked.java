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
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;

@Manifest
public class HumanTaskRevoked extends HumanTaskTransitioned {
    /**
     * New assignee of the task
     */
    public final String assignee; // assignee of the task

    private enum Fields {
        assignee
    }

    public HumanTaskRevoked(HumanTask task) {
        super(task, task.getImplementation().getPreviousTaskState(), TaskAction.Revoke);
        this.assignee = task.getImplementation().getPreviousAssignee();
    }

    public HumanTaskRevoked(ValueMap json) {
        super(json);
        this.assignee = readField(json, Fields.assignee);
    }

    public void updateState(WorkflowTask task) {
        task.revoke();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTransitionEvent(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
