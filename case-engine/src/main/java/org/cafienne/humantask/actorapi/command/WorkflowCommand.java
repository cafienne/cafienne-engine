package org.cafienne.humantask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.exception.AuthorizationException;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.State;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;
import java.util.Arrays;

public abstract class WorkflowCommand extends CaseCommand {
    private final String taskId;
    private HumanTask task;

    protected WorkflowCommand(TenantUser tenantUser, String caseInstanceId, String taskId) {
        super(tenantUser, caseInstanceId);
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new NullPointerException("Task id should not be null or empty");
        }

        this.taskId = taskId;
    }

    protected WorkflowCommand(ValueMap json) {
        super(json);
        this.taskId = readField(json, Fields.taskId);
    }

    protected String getTaskId() {
        return taskId;
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        // Now get the plan item ...
        PlanItem planItem = caseInstance.getPlanItemById(taskId);
        if (planItem == null) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + ": The task with id " + taskId + " could not be found in case " + caseInstance.getId());
        }
        // ... and validate that it's a human task
        if (!(planItem instanceof HumanTask)) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + ": The plan item with id " + planItem.getId() + " in case " + caseInstance.getId() + " is not a HumanTask");
        }
        // Good. It's a HumanTask
        task = (HumanTask) planItem;

        // TODO: validate that this is a proper check (e.g. why could i not Delegate a suspended or failed task??)
        State currentState = task.getState();
        if (currentState != State.Active) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + " cannot be done because task " + planItem.getName() + " (" + taskId + ") is not in Active but in " + currentState + " state");
        }

        // Now have the actual command do its own validation on the task
        validate(task);
    }

    public abstract void validate(HumanTask task) throws InvalidCommandException;

    public ModelResponse process(Case caseInstance) {
        return process(task.getImplementation());
    }

    public abstract ModelResponse process(WorkflowTask task);

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
        for (int i = 0; i < expectedStates.length; i++) {
            if (expectedStates[i].equals(currentTaskState)) {
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
        if (! task.getCaseInstance().getCurrentTeamMember().isOwner()) {
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
        if (task.getCaseInstance().getCurrentTeamMember().isOwner()) {
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

    protected void validateCaseTeamMembership(HumanTask task, String assignee) {
        if (task.getCaseInstance().getCurrentTeamMember().isOwner()) {
            // Case owners will add the team member themselves when assigning/delegating; no need to check membership.
            return;
        }
        // Validate that the new assignee is part of the team
        Member member = task.getCaseInstance().getCaseTeam().getMember(new MemberKey(assignee, "user"));
        if (member == null) {
            raiseException("There is no case team member with id '" + assignee + "'");
        }
        // Validate that - if the task needs a role - the new assignee has that role
        CaseRoleDefinition role = task.getPerformer();
        if (role != null) {
            // Members need to have the role, Owners don't need to
            if (!member.isOwner() && !member.getRoles().contains(role)) {
                raiseAuthorizationException("The case team member with id '" + assignee + "' does not have the case role " + role.getName());
            }
        }
    }
}
