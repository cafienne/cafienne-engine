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
import org.cafienne.json.ValueMap;

/**
 * This is the basis for tasks that can be executed during while the task is alive.
 * Examples: assign, claim, delegate, revoke, etc.
 */
public abstract class TaskManagementCommand extends WorkflowCommand {
    protected TaskManagementCommand(CaseUserIdentity user, String caseInstanceId, String taskId) {
        super(user, caseInstanceId, taskId);
    }

    protected TaskManagementCommand(ValueMap json) {
        super(json);
    }

    /**
     * Task management needs the user to have the case role.
     * The only situation where this is not required is when an assignee without the case role revokes a task.
     * This is typically when the task has been delegated before to that user.
     */
    protected boolean requiresCaseRole() {
        return true;
    }

    @Override
    public final void validate(HumanTask task) {
        if (requiresCaseRole() && !task.getCaseInstance().getCurrentTeamMember().hasRole(task.getDefinition().getPerformer())) {
            raisePermissionException();
        }
        // TODO: Management of the task should also be possible when the task is suspended. However, this currently
        //   is not possible, as the HumanTaskEvents do not understand the notion of history state
        //   and also the projections change the task-state when a task is suspended where it should not be done.
        //   Therefore now only checking that the task is active.
        // validateState(task, TaskState.Unassigned, TaskState.Assigned, TaskState.Delegated, TaskState.Suspended);

        mustBeActive(task);

        // Now run the command specific validations.
        validateTaskAction(task);
    }

    protected abstract void validateTaskAction(HumanTask task);
}
