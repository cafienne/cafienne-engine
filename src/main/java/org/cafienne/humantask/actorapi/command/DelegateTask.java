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

package org.cafienne.humantask.actorapi.command;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class DelegateTask extends AssignTask {
    public DelegateTask(CaseUserIdentity user, String caseInstanceId, String taskId, CaseUserIdentity delegatee) {
        super(user, caseInstanceId, taskId, delegatee);
    }

    public DelegateTask(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(HumanTask task) {
        super.validateTaskOwnership(task);
        super.validateState(task, TaskState.Assigned);
        super.validateCaseTeamMembership(task, assignee);
    }

    @Override
    public void processWorkflowCommand(WorkflowTask workflowTask) {
        workflowTask.delegate(assignee);
    }
}
