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
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskAssigned;
import org.cafienne.humantask.akka.event.HumanTaskOwnerChanged;
import org.cafienne.humantask.instance.TaskState;

import java.io.IOException;

@Manifest
public class AssignTask extends HumanTaskCommand {
    private final String assignee;

    public AssignTask(TenantUser tenantUser, String caseInstanceId, String taskId, String assignee) {
        super(tenantUser, caseInstanceId, taskId);
        this.assignee = assignee;
        if (assignee == null || assignee.trim().isEmpty()) {
            throw new InvalidCommandException("AssignTask: The assignee should not be null or empty");
        }
    }

    public AssignTask(ValueMap json) {
        super(json);
        this.assignee = readField(json, Fields.assignee);
    }

    @Override
    public void validate(HumanTask task) {
        super.validateCaseOwnership(task);
        super.validateState(task, TaskState.Unassigned);
    }

    @Override
    public HumanTaskResponse process(HumanTask task) {
        task.addEvent(new HumanTaskAssigned(task, this.assignee));
        task.addEvent(new HumanTaskOwnerChanged(task, this.assignee));
        return new HumanTaskResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
