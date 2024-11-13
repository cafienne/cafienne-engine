/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.humantask.actorapi.command;

import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.humantask.instance.TaskState;
import com.casefabric.humantask.instance.WorkflowTask;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

@Manifest
public class ClaimTask extends TaskManagementCommand {
    public ClaimTask(CaseUserIdentity user, String caseInstanceId, String taskId) {
        super(user, caseInstanceId, taskId);
    }

    public ClaimTask(ValueMap json) {
        super(json);
    }

    @Override
    public void validateTaskAction(HumanTask task) {
        verifyTaskPairRestrictions(task, getUser());
    }

    @Override
    public void processTaskCommand(WorkflowTask workflowTask) {
        workflowTask.claim();
    }
}
