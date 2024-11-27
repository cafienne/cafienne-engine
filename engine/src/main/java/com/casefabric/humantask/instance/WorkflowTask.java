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

package com.casefabric.humantask.instance;

import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.actormodel.identity.Origin;
import com.casefabric.cmmn.actorapi.command.platform.NewUserInformation;
import com.casefabric.cmmn.actorapi.event.CaseAppliedPlatformUpdate;
import com.casefabric.cmmn.definition.HumanTaskDefinition;
import com.casefabric.cmmn.definition.extension.workflow.AssignmentDefinition;
import com.casefabric.cmmn.definition.extension.workflow.DueDateDefinition;
import com.casefabric.cmmn.definition.extension.workflow.TaskModelDefinition;
import com.casefabric.cmmn.definition.extension.workflow.WorkflowTaskDefinition;
import com.casefabric.cmmn.definition.team.CaseRoleDefinition;
import com.casefabric.cmmn.instance.CMMNElement;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.cmmn.instance.task.validation.ValidationError;
import com.casefabric.cmmn.instance.task.validation.ValidationResponse;
import com.casefabric.humantask.actorapi.event.*;
import com.casefabric.humantask.actorapi.event.migration.HumanTaskMigrated;
import com.casefabric.json.StringValue;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

import java.time.Instant;
import java.util.Objects;

public class WorkflowTask extends CMMNElement<WorkflowTaskDefinition> {
    private final HumanTask task;

    private String currentOwner = "";
    private String currentAssignee = "";
    private Instant currentDueDate = null;

    private TaskState currentTaskState = TaskState.Null;
    private TaskState historyTaskState = TaskState.Null;

    public WorkflowTask(WorkflowTaskDefinition workflowTaskDefinition, HumanTask humanTask) {
        super(humanTask, workflowTaskDefinition);
        this.task = humanTask;
    }

    public HumanTask getTask() {
        return task;
    }

    private Value<?> getTaskModel() {
        // If the task model is defined, then bind it to the task input parameters.
        // If the task model is not defined, just return an empty json string.
        TaskModelDefinition definition = getDefinition().getTaskModel();
        if (definition != null) {
            return getDefinition().getTaskModel().getValue(task);
        } else {
            return new StringValue("");
        }
    }

    private String getPerformerRole() {
        CaseRoleDefinition performer = task.getDefinition().getPerformer();
        return performer != null ? performer.getName() : "";
    }

    public void beginLifeCycle() {
        ValueMap inputParameters = task.getMappedInputParameters();
        // Inform that we have become active
        addEvent(new HumanTaskActivated(task, getPerformerRole(), getTaskModel()));
        // Try to assign and fill due date based on custom definition fields
        calculateOptionalAssignment();
        calculateOptionalDueDate();
        // Inform about task input
        addEvent(new HumanTaskInputSaved(task, inputParameters));
    }

    private void calculateOptionalAssignment() {
        AssignmentDefinition assignment = getDefinition().getAssignmentExpression();
        if (assignment != null) {
            try {
                String newAssignee = assignment.evaluate(this.task);
                addDebugInfo(() -> "Assignee expression in task " + task.getName() + "[" + task.getId() + " resulted in: " + newAssignee);

                // Whether or not assignee is an existing tenant user cannot be determined here...
                //  It is up to the application using the dynamic assignment to make sure it returns a valid user.
                //  If not a valid user, then case owners have to change the task assignee.
                if (newAssignee != null && !newAssignee.trim().isEmpty()) {
                    assign(CaseUserIdentity.apply(newAssignee, Origin.IDP));
                }
            } catch (Exception e) {
                addDebugInfo(() -> "Failed to evaluate expression to assign task", e);
            }
        }
    }

    private void calculateOptionalDueDate() {
        DueDateDefinition dueDateExpression = getDefinition().getDueDateExpression();
        if (dueDateExpression != null) {
            try {
                Instant dueDate = dueDateExpression.evaluate(this.task);
                addDebugInfo(() -> "Due date expression in task " + task.getName() + "[" + task.getId() + " resulted in: " + dueDate);
                setDueDate(dueDate);
            } catch (Exception e) {
                addDebugInfo(() -> "Failed to evaluate expression on task due date", e);
            }
        }
    }

    public void updateState(HumanTaskAssigned event) {
        this.currentAssignee = event.assignee;
    }

    public void updateState(HumanTaskOwnerChanged event) {
        this.currentOwner = event.owner;
    }

    public void updateState(HumanTaskTransitioned event) {
        this.historyTaskState = event.getHistoryState();
        this.currentTaskState = event.getCurrentState();
    }

    public void updateState(HumanTaskDueDateFilled event) {
        this.currentDueDate = event.dueDate;
    }

    /**
     * Get the current task assignee
     *
     * @return current task assignee
     */
    public String getAssignee() {
        return currentAssignee;
    }

    /**
     * Get the current task state
     *
     * @return current task state [Unassigned, Assigned, Delegated]
     */
    public TaskState getCurrentState() {
        return currentTaskState;
    }

    /**
     * Get the previous task state
     *
     * @return previous task state [Unassigned, Assigned, Delegated]
     */
    public TaskState getHistoryState() {
        return historyTaskState;
    }

    private boolean isNewAssignee(CaseUserIdentity newAssignee) {
        return !Objects.equals(this.currentAssignee, newAssignee.id());
    }

    private void addCaseTeamUser(CaseUserIdentity newMember, CaseRoleDefinition role) {
        getCaseInstance().getCaseTeam().upsertCaseTeamUser(newMember, role);
    }

    public void assign(CaseUserIdentity newAssignee) {
        if (isNewAssignee(newAssignee)) {
            addCaseTeamUser(newAssignee, task.getPerformer());
            addEvent(new HumanTaskAssigned(task, newAssignee.id()));
            checkOwnershipChange(newAssignee.id());
        }
    }

    public void claim() {
        CaseUserIdentity claimer = getCaseInstance().getCurrentUser();
        if (isNewAssignee(claimer) || currentTaskState != TaskState.Assigned) {
            addEvent(new HumanTaskClaimed(task, claimer.id()));
            checkOwnershipChange(claimer.id());
        }
    }

    public void delegate(CaseUserIdentity newAssignee) {
        if (isNewAssignee(newAssignee)) {
            addCaseTeamUser(newAssignee, null);
            addEvent(new HumanTaskDelegated(task, newAssignee.id()));
        }
    }

    public void revoke() {
        // When a task is revoked, it gets assigned to the previous assignee.
        //  - If the task is in Delegated state, it means the original assignee delegated it to someone else,
        //    and now the delegate revokes the task, so the task again gets assigned to the original owner.
        //  - If the task is in Assigned state, it means the assignee revokes, and the task goes back to Unassigned
        //    state. This means also that the owner should be removed;
        if (currentTaskState == TaskState.Unassigned) {
            return;
        }

        TaskState nextState = currentTaskState == TaskState.Delegated ? TaskState.Assigned : TaskState.Unassigned;
        String nextAssignee = currentTaskState == TaskState.Delegated ? currentOwner : "";

        addEvent(new HumanTaskRevoked(task, nextAssignee, nextState, TaskAction.Revoke));
        checkOwnershipChange(nextAssignee);
    }

    private void checkOwnershipChange(String newOwner) {
        if (Objects.equals(this.currentOwner, newOwner)) return;
        addEvent(new HumanTaskOwnerChanged(task, newOwner));
    }

    public boolean complete(ValueMap taskOutput) {
        // TTDL
        // First validate task output. Note: this may result in "CommandException", instead of "InvalidCommandException". For that reason this cannot be done right now in validate method.
        ValidationResponse validate = task.validateOutput(taskOutput);
        if (validate instanceof ValidationError) {
            throw new InvalidCommandException("Output for task " + task.getName() + " could not be validated due to an error", validate.getException());
        } else {
            if (!validate.getContent().getValue().isEmpty()) {
                throw new InvalidCommandException("Output for task " + task.getName() + " is invalid\n" + validate.getContent());
            }
        }

        // If there is no assignee, or the assignee is someone else than the user completing the task,
        //  then the task is claimed implicitly by the current user.
        CaseUserIdentity user = getCaseInstance().getCurrentUser();
        if (isNewAssignee(user)){
            addEvent(new HumanTaskClaimed(task, user.id()));
            checkOwnershipChange(user.id());
        }

        addEvent(new HumanTaskCompleted(task, taskOutput));
        // This will generate PlanItemTransitioned and CaseFileItem events
        return task.goComplete(taskOutput);
    }

    public void setDueDate(Instant newDueDate) {
        if (newDueDate != null && !Objects.equals(this.currentDueDate, newDueDate)) {
            addEvent(new HumanTaskDueDateFilled(task, newDueDate));
        }
    }

    public void saveOutput(ValueMap taskOutput) {
        addEvent(new HumanTaskOutputSaved(task, taskOutput));
    }

    public void updateState(CaseAppliedPlatformUpdate event) {
        NewUserInformation updatedAssignee = event.newUserInformation.getUserUpdate(this.currentAssignee);
        if (updatedAssignee != null) {
            addDebugInfo(() -> "Updating assignee of " + this.task + " with new user id " + updatedAssignee.newUserId());
            this.currentAssignee = updatedAssignee.newUserId();
        }
        NewUserInformation updatedOwner = event.newUserInformation.getUserUpdate(this.currentOwner);
        if (updatedOwner != null) {
            addDebugInfo(() -> "Updating owner of " + this.task + " with new user id " + updatedOwner.newUserId());
            this.currentOwner = updatedOwner.newUserId();
        }
    }

    @Override
    public void migrateDefinition(WorkflowTaskDefinition newDefinition, boolean skipLogic) {
        super.migrateDefinition(newDefinition, skipLogic);
        if (skipLogic) return;

        if (currentTaskState == TaskState.Null) {
            // Task has not yet been activated, and has not yet published any events. No need to do any migration.
            return;
        }

        if (hasNewName() || hasNewPerformerRole() || hasNewTaskModel()) {
            addEvent(new HumanTaskMigrated(this.task, getPerformerRole(), getTaskModel()));
            // There may be a new assignment or due date from the definition
            // Currently this gives issues, e.g. because new different due date may not be caused by
            //  different expression (i.e., by the new definition), but simply by re-evaluating the expression.
            // Therefore not catering for such kind of changes currently.
            //  It actually requires a definition comparison (e.g., an implementation of equals and differences on CMMNElementDefinition)
        }
    }

    private boolean hasNewName() {
        return !task.getPreviousDefinition().getName().equals(task.getDefinition().getName());
    }

    private String getRoleName(HumanTaskDefinition htd) {
        return htd.getPerformer() != null ? htd.getPerformer().getName() : null;
    }

    private boolean hasNewPerformerRole() {
        String formerPerformerName = getRoleName(task.getPreviousDefinition());
        String currentPerformerName = getRoleName(task.getDefinition());
        return !Objects.equals(formerPerformerName, currentPerformerName);
    }

    private boolean hasNewTaskModel() {
        return !Objects.equals(getPreviousDefinition().getTaskModel(), getDefinition().getTaskModel());
    }
}
