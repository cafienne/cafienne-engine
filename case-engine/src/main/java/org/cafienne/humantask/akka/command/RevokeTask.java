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
import org.cafienne.humantask.akka.event.HumanTaskOwnerChanged;
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
        String currentTaskAssignee = task.getImplementation().getAssignee();
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
        String previousAssignee = task.getImplementation().getPreviousAssignee();
        task.addEvent(new HumanTaskRevoked(task, previousAssignee));

        // When a task is revoked, it get's assigned to the previous assignee.
        //  - If the task is in Delegated state, it means the original assignee delegated it to someone else,
        //    and now the delegatee revokes the task, so the task again get's assigned to the original owner.
        //  - If the task is in Assigned state, it means the assignee revokes, and the task goes back to Unassigned
        //    state. This means also that the owner should be removed;
        // Here we check whether the task owner changes, and if so, we add an event for it.
        if (!task.getImplementation().getOwner().equals(previousAssignee)) {
            task.addEvent(new HumanTaskOwnerChanged(task, previousAssignee));
        }
        return new HumanTaskResponse(this);
    }
}
