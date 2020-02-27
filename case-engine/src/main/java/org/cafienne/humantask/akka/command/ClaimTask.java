/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskClaimed;
import org.cafienne.humantask.instance.TaskState;

@Manifest
public class ClaimTask extends WorkflowCommand {
    public ClaimTask(TenantUser tenantUser, String caseInstanceId, String taskId) {
        super(tenantUser, caseInstanceId, taskId);
    }

    public ClaimTask(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(HumanTask task) {
        /*
         * String currentUser = getUser().id();
         * TODO: 1. Check whether the current user has the privilege to claim the task
         * TODO: 2. Check whether the current user is part of CaseTeam. If not what to do?
         */

        if (!task.currentUserIsAuthorized()) {
            throw new SecurityException("No permission to perform this task");
        }

        validateState(task, TaskState.Unassigned);
    }
    
    @Override
    public HumanTaskResponse process(HumanTask task) {
        task.addEvent(new HumanTaskClaimed(task, this.user.id())).updateState(task.getImplementation());
        return new HumanTaskResponse(this);
    }
}
