/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskAssigned;
import org.cafienne.humantask.instance.TaskState;

import java.io.IOException;

@Manifest
public class AssignTask extends WorkflowCommand {
    private final String assignee;

    private enum Fields {
        assignee
    }

    public AssignTask(TenantUser tenantUser, String caseInstanceId, String taskId, String assignee) {
        super(tenantUser, caseInstanceId, taskId);
        this.assignee = assignee;
    }

    public AssignTask(ValueMap json) {
        super(json);
        this.assignee = readField(json, Fields.assignee);
    }

    @Override
    public void validate(HumanTask task) {
        if (assignee == null || assignee.trim().isEmpty()) {
            throw new InvalidCommandException("AssignTask: The assignee should not be null or empty");
        }
        validateState(task, TaskState.Unassigned);
        // TODO: 1. Validate whether assignee is a valid user in the system
        // TODO: 2. Check whether the current user has the privilege to assign the task to assignee
    }

    @Override
    public HumanTaskResponse process(HumanTask task) {
        task.addEvent(new HumanTaskAssigned(task, this.assignee)).updateState(task.getImplementation());
        return new HumanTaskResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
