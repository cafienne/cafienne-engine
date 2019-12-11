/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskRevoked;
import org.cafienne.humantask.instance.TaskState;

@Manifest
public class RevokeTask extends WorkflowCommand {
    public RevokeTask(TenantUser tenantUser, String caseInstanceId, String taskId) {
        super(tenantUser, caseInstanceId, taskId);
    }

    public RevokeTask(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(HumanTask task) {
        String currentTaskAssignee = task.getImplementation().getTaskAssignee();
        if (currentTaskAssignee == null || currentTaskAssignee.trim().isEmpty()) {
            throw new InvalidCommandException("RevokeTask: Only Assigned or Delegated task can be revoked");
        }

        String currentUserId = getUser().id();
        if (!currentUserId.equals(currentTaskAssignee)) {
            throw new InvalidCommandException("RevokeTask: Only the current task assignee (" + currentTaskAssignee + ") can revoke the task (" + task.getId() + ")");
        }
        validateState(task, TaskState.Assigned, TaskState.Delegated);
    }

    @Override
    public HumanTaskResponse process(HumanTask task) {
        task.addEvent(new HumanTaskRevoked(task)).updateState(task.getImplementation());
        return new HumanTaskResponse(this);
    }
}
