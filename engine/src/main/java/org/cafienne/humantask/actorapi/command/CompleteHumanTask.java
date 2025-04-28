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
import org.cafienne.cmmn.instance.Task;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * This command must be used to complete a human task with additional task output parameters.
 */
@Manifest
public class CompleteHumanTask extends TaskOutputCommand {
    protected Task<?> task;

    /**
     * Create a command to complete the human task with the specified id to complete.
     * If the plan item is not a task or if no plan item can be found, a CommandFailure will be returned.
     *
     * @param caseInstanceId
     * @param taskId     The id of the task. In general it is preferred to select a plan item by id, rather than by name. If the task id is null or left empty,
     *                   then the value of the name parameter will be considered.
     * @param taskOutput An optional map with named output parameters for the task. These will be set on the task before the task is reported as complete. This
     *                   means that the parameters will also be bound to the case file, which may cause sentries to activate before the task completes.
     */
    public CompleteHumanTask(CaseUserIdentity user, String caseInstanceId, String rootCaseId, String taskId, ValueMap taskOutput) {
        super(user, caseInstanceId, rootCaseId, taskId, taskOutput);
    }

    public CompleteHumanTask(ValueMap json) {
        super(json);
    }

    @Override
    public void processTaskCommand(WorkflowTask workflowTask) {
        workflowTask.complete(taskOutput);
    }

    @Override
    public String toString() {
        String taskName = task != null ? task.getName() + " with id " + getTaskId() : getTaskId() + " (unknown name)";
        return "Complete HumanTask '" + taskName + "' with output\n" + taskOutput;
    }
}
