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

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.exception.AuthorizationException;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.definition.extension.workflow.FourEyesDefinition;
import com.casefabric.cmmn.definition.extension.workflow.RendezVousDefinition;
import com.casefabric.cmmn.definition.extension.workflow.TaskPairingDefinition;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.PlanItem;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.humantask.actorapi.response.HumanTaskResponse;
import com.casefabric.humantask.instance.TaskState;
import com.casefabric.humantask.instance.WorkflowTask;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class HumanTaskCommand extends CaseCommand {
    private final String taskId;
    private HumanTask task;

    protected HumanTaskCommand(CaseUserIdentity user, String caseInstanceId, String taskId) {
        super(user, caseInstanceId);
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new NullPointerException("Task id should not be null or empty");
        }

        this.taskId = taskId;
    }

    protected HumanTaskCommand(ValueMap json) {
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
        if (!task.getState().isAlive()) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + " cannot be done because task " + planItem.getName() + " (" + taskId + ") is in state " + task.getState());
        }

        // Validate that the current user has the authorization to perform the task
        validateTaskAccess(task);
        // Now have the actual command do its own validation on the task
        validate(task);
    }

    public abstract void validate(HumanTask task) throws InvalidCommandException;

    @Override
    public void processCaseCommand(Case caseInstance) {
        processTaskCommand(task.getImplementation());
        if (getResponse() == null) {
            setResponse(new HumanTaskResponse(this));
        }
    }

    public abstract void processTaskCommand(WorkflowTask task);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
    }

    /**
     * Helper method that validates whether a task is in one of the expected states.
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
        TaskState currentTaskState = task.getImplementation().getCurrentState();
        if (!currentTaskState.isActive()) {
            raiseException("Cannot be done because the task is not Active but " + currentTaskState);
        }
    }

    private List<HumanTask> getReferencedTasks(TaskPairingDefinition taskPairingDefinition) {
        // Build up the list of all HumanTasks that have been worked on by a user
        //  and that is referenced within the TaskPairingDefinition (either "rendez-vous" or "four-eyes").
        //  NOTE (!): the list contains ALL plan items, including repetitive ones.
        //  But, since the order of that list is by creation, the last one that was acted upon for a repetitive one is taken into account, not all of them.
        //  We'd have to check if that really works - most probably it means that after you've completed an "approve task",
        //   the next "submit task" cannot be picked up by the same user.
        //  We may find a solution around this by checking for repetitive items whether they're in the same stage instance or not. That sounds sort of logical.
        //  But, before coding all of that, let's await a use case were someone actually bumps into this...
        // So for now we keep it kinda simple.
        return task.getCaseInstance().getPlanItems().stream()
                .filter(item -> taskPairingDefinition.references(item.getItemDefinition())) // whether we reference them from our task
                .filter(item -> item instanceof HumanTask).map(item -> (HumanTask) item) // and only HumanTasks as of now
                .filter(item -> item.getState().isInitiated()) // only tasks that have been activated ...
                .filter(item -> item.getImplementation().getCurrentState().isWorkedOn()) // ... and that have been worked on
                .collect(Collectors.toList());
    }

    protected void verifyTaskPairRestrictions(HumanTask task, CaseUserIdentity ...user) {
        String userId = user.length > 0 ? user[0].id() : getUser().id();
        if (task.getItemDefinition().hasFourEyes()) {
            FourEyesDefinition verificationRules = task.getItemDefinition().getFourEyesDefinition();
            List<HumanTask> items = getReferencedTasks(verificationRules);
            task.getCaseInstance().addDebugInfo(() -> {
                ValueMap logs = new ValueMap("FourEyes Verification on " + task.getName(), "Checking " + getClass().getSimpleName() +" constraints for user " + userId);
                logs.plus(" Related tasks", "Four eyes has been defined for " + verificationRules.getAllReferredItemNames());
                String resultTag = " Verification result";
                ValueMap details = logs.with(resultTag);
                if (items.isEmpty()) {
                    details.plus("ok", "none of the defined elements has been assigned yet, so no need for four eyes check");
                } else {
                    // Log for each item it's comparison
                    items.forEach(item -> {
                        String result = item.getImplementation().getAssignee().equals(userId) ? "check fails   " : "check succeeds";
                        details.plus(item.getName(), result + " - task is assigned to " + item.getImplementation().getAssignee());
                    });
                    // Log items that have not been selected for comparison as well, as they are probably not yet assigned
                    verificationRules.getAllReferences().forEach(item -> {
                        if (items.stream().map(HumanTask::getItemDefinition).noneMatch(any -> any.equals(item))) {
                            details.plus(item.getName(), "check succeeds - task has not yet been assigned");
                        }
                    });
                }
                return logs;
            });
            items.forEach(item -> {
                if (item.getImplementation().getAssignee().equals(userId)) {
                    raiseAuthorizationException("Since you have worked on " + item.getName() + " you cannot also work on " + task.getName());
                }
            });
        } else {
            task.getCaseInstance().addDebugInfo(() -> "FourEyes Verification is not defined for task " + task.getName());
        }

        if (task.getItemDefinition().hasRendezVous()) {
            RendezVousDefinition verificationRules = task.getItemDefinition().getRendezVousDefinition();
            List<HumanTask> items = getReferencedTasks(verificationRules);
            task.getCaseInstance().addDebugInfo(() -> {
                ValueMap logs = new ValueMap("RendezVous Verification on " + task.getName(), "Checking " + getClass().getSimpleName() +" constraints for user " + userId);
                logs.plus(" Related tasks", "Rendez-vous has been defined on " + verificationRules.getAllReferredItemNames());
                String resultTag = " Verification result";
                ValueMap details = logs.with(resultTag);
                if (items.isEmpty()) {
                    details.plus("ok", "none of the defined elements has been assigned yet, so no need for rendez vous check");
                } else {
                    items.forEach(item -> {
                        String result = item.getImplementation().getAssignee().equals(userId) ? "check succeeds" : "check fails   ";
                        details.plus(item.getName(), result + " - task is assigned to " + item.getImplementation().getAssignee());
                    });
                    verificationRules.getAllReferences().forEach(item -> {
                        if (items.stream().map(HumanTask::getItemDefinition).noneMatch(any -> any.equals(item))) {
                            details.plus(item.getName(), "check succeeds - task has not yet been assigned");
                        }
                    });
                }
                return logs;
            });
            items.forEach(item -> {
                if (!item.getImplementation().getAssignee().equals(userId)) {
                    raiseAuthorizationException("Since you have not worked on " + item.getName() + " you cannot work on " + task.getName());
                }
            });
        } else {
            task.getCaseInstance().addDebugInfo(() -> "RendezVous Verification is not defined for task " + task.getName());
        }
    }

    /**
     * Tasks are "owned" by the assignee and by the case owners
     */
    protected void validateTaskAccess(HumanTask task) {
        // You own the task if you have the right role, or if you're the current assigned person
        if (!task.getCaseInstance().getCurrentTeamMember().hasRole(task.getPerformer())) {
            String currentTaskAssignee = task.getImplementation().getAssignee();
            if (currentTaskAssignee == null || currentTaskAssignee.isEmpty()) {
                raisePermissionException();
            } else {
                String currentUserId = getUser().id();
                if (!currentUserId.equals(currentTaskAssignee)) {
                    raisePermissionException();
                }
            }
        }
    }

    protected void raisePermissionException() {
        raiseAuthorizationException("You do not have permission to perform this operation");
    }

    protected void raiseAuthorizationException(String msg) {
        throw new AuthorizationException(this.getClass().getSimpleName() + "[" + getTaskId() + "]: " + msg);
    }

    protected void raiseException(String msg) {
        throw new InvalidCommandException(this.getClass().getSimpleName() + "[" + getTaskId() + "]: " + msg);
    }
}
