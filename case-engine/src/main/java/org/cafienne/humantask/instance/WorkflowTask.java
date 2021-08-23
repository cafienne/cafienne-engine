package org.cafienne.humantask.instance;

import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation;
import org.cafienne.cmmn.actorapi.event.CaseAppliedPlatformUpdate;
import org.cafienne.cmmn.definition.HumanTaskDefinition;
import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.definition.task.WorkflowTaskDefinition;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.ValidationError;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.cafienne.humantask.actorapi.event.*;
import org.cafienne.humantask.actorapi.event.migration.HumanTaskMigrated;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.time.Instant;
import java.util.Objects;

public class WorkflowTask extends CMMNElement<WorkflowTaskDefinition> {
    private final HumanTask task;

    private String currentOwner = "";
    private String currentAssignee = "";
    private Instant currentDueDate = null;

    private TaskState currentTaskState = TaskState.Null;
    private TaskState historyTaskState = TaskState.Null;
    private TaskAction lastAction = TaskAction.Null;

    public WorkflowTask(WorkflowTaskDefinition workflowTaskDefinition, HumanTask humanTask) {
        super(humanTask, workflowTaskDefinition);
        this.task = humanTask;
    }

    public HumanTask getTask() {
        return task;
    }

    private Value<?> getTaskModel() {
        return getDefinition().getTaskModel();
    }

    private String getPerformerRole() {
        CaseRoleDefinition performer = task.getDefinition().getPerformer();
        return performer != null ? performer.getName() : "";
    }

    public void beginLifeCycle() {
        // Inform that we have become active
        addEvent(new HumanTaskActivated(task, getPerformerRole(), getTaskModel()));
        // Try to assign and fill due date based on custom definition fields
        calculateOptionalAssignment();
        calculateOptionalDueDate();
        // Inform about task input
        addEvent(new HumanTaskInputSaved(task, task.getMappedInputParameters()));
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
                    assign(newAssignee);
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
        this.lastAction = event.getTransition();
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

    private boolean isNewAssignee(String newAssignee) {
        return !Objects.equals(this.currentAssignee, newAssignee);
    }

    private void addCaseTeamMember(String newMember) {
        getCaseInstance().getCaseTeam().upsertCaseTeamMember(newMember, task.getPerformer());
    }

    public void assign(String newAssignee) {
        if (isNewAssignee(newAssignee)) {
            addCaseTeamMember(newAssignee);
            addEvent(new HumanTaskAssigned(task, newAssignee));
            checkOwnershipChange(newAssignee);
        }
    }

    public void claim(String claimer) {
        if (isNewAssignee(claimer)) {
            addEvent(new HumanTaskClaimed(task, claimer));
            checkOwnershipChange(claimer);
        }
    }

    public void delegate(String newAssignee) {
        if (isNewAssignee(newAssignee)) {
            addCaseTeamMember(newAssignee);
            addEvent(new HumanTaskDelegated(task, newAssignee));
        }
    }

    public void revoke() {
        // When a task is revoked, it get's assigned to the previous assignee.
        //  - If the task is in Delegated state, it means the original assignee delegated it to someone else,
        //    and now the delegatee revokes the task, so the task again get's assigned to the original owner.
        //  - If the task is in Assigned state, it means the assignee revokes, and the task goes back to Unassigned
        //    state. This means also that the owner should be removed;

        TaskState nextState = currentTaskState == TaskState.Delegated ? TaskState.Assigned : TaskState.Unassigned;
        String nextAssignee = currentTaskState == TaskState.Delegated ? currentOwner : "";

        addEvent(new HumanTaskRevoked(task, nextAssignee, nextState, TaskAction.Revoke));
        checkOwnershipChange(nextAssignee);
    }

    private void checkOwnershipChange(String newOwner) {
        if (this.currentOwner.equalsIgnoreCase(newOwner)) return;
        addEvent(new HumanTaskOwnerChanged(task, newOwner));
    }

    public void complete(ValueMap taskOutput) {
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

        addEvent(new HumanTaskCompleted(task, taskOutput));
        // This will generate PlanItemTransitioned and CaseFileItem events
        task.goComplete(taskOutput);
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
    public void migrateDefinition(WorkflowTaskDefinition newDefinition) {
        super.migrateDefinition(newDefinition);
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
        return !getPreviousDefinition().getTaskModel().equals(getDefinition().getTaskModel());
    }
}
