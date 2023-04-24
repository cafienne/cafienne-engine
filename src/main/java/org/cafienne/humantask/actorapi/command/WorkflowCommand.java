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

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Arrays;

public abstract class WorkflowCommand extends CaseCommand {
    private final String taskId;
    private HumanTask task;

    protected WorkflowCommand(CaseUserIdentity user, String caseInstanceId, String taskId) {
        super(user, caseInstanceId);
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new NullPointerException("Task id should not be null or empty");
        }

        this.taskId = taskId;
    }

    protected WorkflowCommand(ValueMap json) {
        super(json);
        this.taskId = json.readString(Fields.taskId);
    }

    protected String getTaskId() {
        return taskId;
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        // Now get the plan item ...
        PlanItem<?> planItem = caseInstance.getPlanItemById(taskId);
        if (planItem == null) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + ": The task with id " + taskId + " could not be found in case " + caseInstance.getId());
        }
        // ... and validate that it's a human task
        if (!(planItem instanceof HumanTask)) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + ": The plan item with id " + planItem.getId() + " in case " + caseInstance.getId() + " is not a HumanTask");
        }
        // Good. It's a HumanTask
        task = (HumanTask) planItem;

        // Commands can be sent when the task is Active or Suspended or Failed (and when Enabled/Disabled, but that is hardly in use).
        State currentState = task.getState();
        if ((currentState.isSemiTerminal() && currentState != State.Failed) || currentState == State.Null || currentState == State.Available) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + " cannot be done because task " + planItem.getName() + " (" + taskId + ") is in state " + currentState);
        }

        // Now have the actual command do its own validation on the task
        validate(task);
    }

    public abstract void validate(HumanTask task) throws InvalidCommandException;

    @Override
    public void processCaseCommand(Case caseInstance) {
        processWorkflowCommand(task.getImplementation());
        if (getResponse() == null) {
            setResponse(new HumanTaskResponse(this));
        }
    }

    public abstract void processWorkflowCommand(WorkflowTask task);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
    }

    /**
     * Helper method that validates whether a task is in one of the expected states.
     *
     * @param task
     * @param expectedStates
     */
    protected void validateState(HumanTask task, TaskState... expectedStates) {
        TaskState currentTaskState = task.getImplementation().getCurrentState();
        for (TaskState expectedState : expectedStates) {
            if (expectedState.equals(currentTaskState)) {
                return;
            }
        }
        raiseException("Cannot be done because the task is in " + currentTaskState + " state, but should be in any of " + Arrays.asList(expectedStates) + " state");
    }

    protected void mustBeActive(HumanTask task) {
        validateProperCaseRole(task);

        TaskState currentTaskState = task.getImplementation().getCurrentState();
        if (!currentTaskState.isActive()) {
            raiseException("Cannot be done because the task is not Active but " + currentTaskState);
        }
    }

    /**
     * Validate that the current user is owner to the case
     * @param task
     */
    protected void validateCaseOwnership(HumanTask task) {
        if (! task.getCaseInstance().getCurrentTeamMember().isRoleManager(task.getPerformer())) {
            raiseAuthorizationException("You must be case owner to perform this operation");
        }
    }

    /**
     * Validate that the current user has the proper role in the case team to perform the task
     * @param task
     */
    protected void validateProperCaseRole(HumanTask task) {
        if (!task.currentUserIsAuthorized()) {
            raiseAuthorizationException("You do not have permission to perform this operation");
        }
    }

    /**
     * Tasks are "owned" by the assignee and by the case owners
     * @param task
     */
    protected void validateTaskOwnership(HumanTask task) {
        if (task.getCaseInstance().getCurrentTeamMember().isRoleManager(task.getPerformer())) {
            // case owners have the privilege to do this too....
            return;
        }

        String currentTaskAssignee = task.getImplementation().getAssignee();
        if (currentTaskAssignee == null || currentTaskAssignee.isEmpty()) {
            validateProperCaseRole(task);
        } else {
            String currentUserId = getUser().id();
            if (!currentUserId.equals(currentTaskAssignee)) {
                raiseAuthorizationException("You do not have permission to perform this operation");
            }
        }
    }

    protected void raiseAuthorizationException(String msg) {
        throw new AuthorizationException(this.getClass().getSimpleName() + "[" + getTaskId() + "]: "+msg);
    }

    protected void raiseException(String msg) {
        throw new InvalidCommandException(this.getClass().getSimpleName() + "[" + getTaskId() + "]: "+msg);
    }
}
