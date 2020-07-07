package org.cafienne.humantask.instance;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.definition.HumanTaskDefinition;
import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.definition.task.WorkflowTaskDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.ValidationError;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.cafienne.humantask.akka.event.*;

import java.time.Instant;

public class WorkflowTask extends CMMNElement<WorkflowTaskDefinition> {
    private final HumanTask task;
    private final WorkflowTaskDefinition definition;

    private String owner = "";
    private String assignee = "";

    private TaskState currentTaskState = TaskState.Null;
    private TaskState historyTaskState = TaskState.Null;
    private TaskAction lastAction = TaskAction.Null;

    public WorkflowTask(WorkflowTaskDefinition workflowTaskDefinition, HumanTask humanTask) {
        super(humanTask, workflowTaskDefinition);
        this.task = humanTask;
        this.definition = workflowTaskDefinition;
    }

    public HumanTask getTask() {
        return task;
    }

    public void beginLifeCycle() {
        // Take the role of the task, and add that to the event
        HumanTaskDefinition htd = definition.getParentElement();
        CaseRoleDefinition performer = htd.getPerformer();
        String performerRole = performer != null ? performer.getName() : "";

        task.addEvent(new HumanTaskActivated(task, performerRole, definition.getTaskModel()));

        // Now check if we can already assign and fill due date
        AssignmentDefinition assignment = getDefinition().getAssignmentExpression();
        if (assignment != null) {
            try {
                String assignee = assignment.evaluate(this.task);
                addDebugInfo(() -> "Assignee expression in task " + task.getName() + "[" + task.getId() + " resulted in: " + assignee);

                // Whether or not assignee is an existing tenant user cannot be determined here...
                //  It is up to the application using the dynamic assignment to make sure it returns a valid user.
                //  If not a valid user, then case owners have to change the task assignee.
                if (assignee != null && !assignee.trim().isEmpty()) {
                    getCaseInstance().getCaseTeam().addDynamicMember(assignee, task.getPerformer());
                    assign(assignee);
                }
            } catch (Exception e) {
                addDebugInfo(() -> "Failed to evaluate expression to assign task", e);
            }
        }

        DueDateDefinition dueDateExpression = getDefinition().getDueDateExpression();
        if (dueDateExpression != null) {
            try {
                Instant dueDate = dueDateExpression.evaluate(this.task);
                addDebugInfo(() -> "Due date expression in task " + task.getName() + "[" + task.getId() + " resulted in: " + dueDate);

                if (dueDate != null) {
                    setDueDate(dueDate);
                }
            } catch (Exception e) {
                addDebugInfo(() -> "Failed to evaluate expression on task due date", e);
            }
        }

        getCaseInstance().addEvent(new HumanTaskInputSaved(task, task.getMappedInputParameters()));
    }

    public void updateState(HumanTaskAssigned event) {
        this.assignee = event.assignee;
    }

    public void updateState(HumanTaskOwnerChanged event) {
        this.owner = event.owner;
    }

    public void updateState(HumanTaskTransitioned event) {
        this.historyTaskState = event.getHistoryState();
        this.currentTaskState = event.getCurrentState();
        this.lastAction = event.getTransition();
    }

    /**
     * Get the current task assignee
     *
     * @return current task assignee
     */
    public String getAssignee() {
        return assignee;
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

    public void assign(String newAssignee) {
        addCaseTeamMember(newAssignee);
        task.addEvent(new HumanTaskAssigned(task, newAssignee));
        checkOwnershipChange(newAssignee);
    }

    private void addCaseTeamMember(String newMember) {
        getCaseInstance().getCaseTeam().addDynamicMember(newMember, task.getPerformer());
    }

    public void claim(String claimer) {
        task.addEvent(new HumanTaskClaimed(task, claimer));
        checkOwnershipChange(claimer);
    }

    public void delegate(String newAssignee) {
        addCaseTeamMember(newAssignee);
        task.addEvent(new HumanTaskDelegated(task, newAssignee));
    }

    public void revoke() {
        // When a task is revoked, it get's assigned to the previous assignee.
        //  - If the task is in Delegated state, it means the original assignee delegated it to someone else,
        //    and now the delegatee revokes the task, so the task again get's assigned to the original owner.
        //  - If the task is in Assigned state, it means the assignee revokes, and the task goes back to Unassigned
        //    state. This means also that the owner should be removed;

        TaskState nextState = currentTaskState == TaskState.Delegated ? TaskState.Assigned : TaskState.Unassigned;
        String nextAssignee = currentTaskState == TaskState.Delegated ? owner : "";

        task.addEvent(new HumanTaskRevoked(task, nextAssignee, nextState, TaskAction.Revoke));
        checkOwnershipChange(nextAssignee);
    }

    private void checkOwnershipChange(String newOwner) {
        if (this.owner.equalsIgnoreCase(newOwner)) return;
        task.addEvent(new HumanTaskOwnerChanged(task, newOwner));
    }

    public void complete(ValueMap taskOutput) {
        // TTDL
        // First validate task output. Note: this may result in "CommandException", instead of "InvalidCommandException". For that reason this cannot be done right now in validate method.
        ValidationResponse validate = task.validateOutput(taskOutput);
        if (validate instanceof ValidationError) {
            throw new InvalidCommandException("Output for task "+task.getName()+" could not be validated due to an error", validate.getException());
        } else {
            if (! validate.getContent().getValue().isEmpty()) {
                throw new InvalidCommandException("Output for task "+task.getName()+" is invalid\n" + validate.getContent());
            }
        }

        task.addEvent(new HumanTaskCompleted(task, taskOutput));
        // This will generate PlanItemTransitioned and CaseFileItem events
        task.goComplete(taskOutput);
    }

    public void setDueDate(Instant newDueDate) {
        task.addEvent(new HumanTaskDueDateFilled(task, newDueDate));
    }

    public void saveOutput(ValueMap taskOutput) {
        task.addEvent(new HumanTaskOutputSaved(task, taskOutput));
    }

    //
//    /**
//     * Appends the workflow task xml to given xml
//     *
//     * @param xmlElem parentElement to which the workflow task xml will be appended
//     */
//    public void dumpMemoryStateToXML(Element xmlElem) {
//        TenantUser user = task.getCurrentUser();
//        String lastModifiedBy = user.id();
//
//        Element paramXML = xmlElem.getOwnerDocument().createElement("WorkflowTask");
//        paramXML.setAttribute("name", definition.getName());
//        paramXML.setAttribute("history", historyTaskState.name());
//        paramXML.setAttribute("state", currentTaskState.name());
//        paramXML.setAttribute("transition", lastAction.name());
//        paramXML.setAttribute("assignee", assignee);
//        paramXML.setAttribute("owner", owner);
//        paramXML.setAttribute("lastModifiedBy", lastModifiedBy);
//        xmlElem.appendChild(paramXML);
//
//        input.getValue().forEach((s, p) -> appendParameter(s, p, paramXML, "input"));
//        // TODO: also print the real task output - but then at the level of the HumanTask, and only after it has become available
//        if (output != null) {
//            output.getValue().forEach((s, p) -> appendParameter(s, p, paramXML, "output"));
//        }
//    }
//    private void appendParameter(String parameterName, Value<?> parameter, Element xmlElem, String tagName) {
//        Object value = parameter.getValue();
//        Element paramXML = xmlElem.getOwnerDocument().createElement(tagName);
//        xmlElem.appendChild(paramXML);
//        paramXML.setAttribute("name", parameterName);
//        Node valueNode = xmlElem.getOwnerDocument().createTextNode(String.valueOf(value));
//        paramXML.appendChild(valueNode);
//    }
//
}
