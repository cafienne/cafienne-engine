package org.cafienne.humantask.instance;

import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.definition.HumanTaskDefinition;
import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.definition.task.WorkflowTaskDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.event.*;

import java.time.Instant;

public class WorkflowTask extends CMMNElement<WorkflowTaskDefinition> {
    private final HumanTask task;
    private final WorkflowTaskDefinition definition;

    private ValueMap input;
    private ValueMap output;

    private String performerRole;
    private ValueMap taskModel;

    private String owner = "";
    private String assignee = "";

    private TaskState currentTaskState = TaskState.Null;
    private TaskState historyTaskState = TaskState.Null;
    private TaskAction lastAction = TaskAction.Null;

    private Instant dueDate;

    public WorkflowTask(WorkflowTaskDefinition workflowTaskDefinition, HumanTask humanTask) {
        super(humanTask, workflowTaskDefinition);
        this.task = humanTask;
        this.definition = workflowTaskDefinition;
    }

    public void start() {

        // Take the role of the task, and add that to the event
        HumanTaskDefinition htd = definition.getParentElement();
        CaseRoleDefinition performer = htd.getPerformer();
        String performerRole = performer != null ? performer.getName() : "";

        task.addEvent(new HumanTaskActivated(task, performerRole, definition.getTaskModel())).updateState(this);

        // Now check if we can already assign and fill due date
        AssignmentDefinition assignment = getDefinition().getAssignmentExpression();
        if (assignment != null) {
            try {
                String assignee = assignment.evaluate(this.task);
                addDebugInfo(() -> "Assignee expression in task " + task.getName() + "[" + task.getId() + " resulted in: " + assignee);

                /**
                 * TODO: Validate assignee?! Against CaseTeam ???
                 */
                if (assignee != null && !assignee.trim().isEmpty()) {
                    task.addEvent(new HumanTaskAssigned(task, assignee)).updateState(this);
                }
            } catch (Exception e) {
                addDebugInfo(DebugEvent.class, d -> d.addMessage("Failed to evaluate expression to assign task", e));
            }
        }

        DueDateDefinition dueDateExpression = getDefinition().getDueDateExpression();
        if (dueDateExpression != null) {
            try {
                Instant dueDate = dueDateExpression.evaluate(this.task);
                addDebugInfo(() -> "Due date expression in task " + task.getName() + "[" + task.getId() + " resulted in: " + dueDate);

                if (dueDate != null) {
                    task.addEvent(new HumanTaskDueDateFilled(task, dueDate)).updateState(this);
                }
            } catch (Exception e) {
                addDebugInfo(DebugEvent.class, d -> d.addMessage("Failed to evaluate expression on task due date", e));
            }
        }
    }

    public void activate(String performerRole, ValueMap taskModel) {
        this.performerRole = performerRole;
        this.taskModel = taskModel;
        makeTransition(TaskAction.Create);
    }

    public void suspend() {
        makeTransition(TaskAction.Suspend);
    }

    public void terminate() {
        makeTransition(TaskAction.Terminate);
    }

    public void resume() {
        makeTransition(TaskAction.Resume);
    }

    /**
     * This method is used to set the task output
     *
     * @param json
     */
    public void setOutput(ValueMap json) {
        this.output = json;
    }

    /**
     * @param dueDate
     */
    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * This method will be called when the HumanTask gets completed
     */
    public void complete() {
        makeTransition(TaskAction.Complete);

        // Complete in parent as well, if one exists.
        task.goComplete(output);
    }

    public void setInput(ValueMap json) {
        this.input = json;
    }

    /**
     * This method is used to assign an Unassigned task to another user (assignee)
     *
     * @param assignee
     */
    public void assign(String assignee) {
        makeTransition(TaskAction.Assign);

        this.assignee = assignee;
        changeOwnerTo(assignee);
    }

    private void changeOwnerTo(String newOwner) {
        task.addEvent(new HumanTaskOwnerChanged(task, newOwner)).updateState(this);
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * This method is called when the user claims a task
     */
    public void claim(String claimer) {
        makeTransition(TaskAction.Claim);

        assignee = claimer;
        changeOwnerTo(claimer);
    }

    /**
     * This method is used to revoke the task
     */
    public void revoke() {
        makeTransition(TaskAction.Revoke);

        if (currentTaskState == TaskState.Assigned) {
            assignee = owner;
        } else {
            changeOwnerTo("");
            assignee = ""; // This means currentState == State.Unassigned
        }
    }

    public String getPreviousAssignee() {
        if (currentTaskState == TaskState.Assigned) {
            return "";
        } else if (currentTaskState == TaskState.Delegated) {
            return owner;
        } else {
            return "";
        }
    }

    /**
     * This method is used to delegate the user's current task to another user (assignee)
     *
     * @param delegate
     */
    public void delegate(String delegate) {
        makeTransition(TaskAction.Delegate);
        assignee = delegate;
    }

    /**
     * Does the transition for the workflow task
     *
     * @param action transition [Claim, Assign, Revoke, Delegate]
     * @throws TransitionDeniedException
     */
    private void makeTransition(TaskAction action) {
//        System.err.println(task.getName()+": making transition "+action+"; current state: "+currentTaskState+", historyState: "+historyTaskState);
        if (!changeState(action)) {
            throw new TransitionDeniedException("Cannot apply action " + action + " on WorkflowHumanTask " + definition.getName() + ", because it is in state " + currentTaskState);
        }
//        System.err.println(task.getName()+": made transition "+action+"; current state: "+currentTaskState+", historyState: "+historyTaskState);
    }

    private void assertState(TaskAction action, TaskState... expectedStates) {
        for (TaskState expectedState : expectedStates) {
            if (this.currentTaskState == expectedState) {
                return;
            }
        }
        if (getCaseInstance().recoveryRunning()) {
            // Ignore if recovery is running; logic is typically reversed; Perhaps if we use Case's StateMachine algorithm this code can be done much cleaner
            return;
        }
        throw new TransitionDeniedException("Cannot apply action " + action + " on task " + definition.getName() + ", because it is in state " + currentTaskState);
    }

    private boolean changeState(TaskAction action) {
        switch (action) {
            case Revoke:
                assertState(action, TaskState.Delegated, TaskState.Assigned);
                TaskState nextState = currentTaskState == TaskState.Delegated ? TaskState.Assigned : TaskState.Unassigned;
                historyTaskState = currentTaskState;
                currentTaskState = nextState;
                lastAction = action;
                break;
            case Claim:
            case Assign:
                // Both claim and assign can only be done in Unassigned state
                assertState(action, TaskState.Unassigned);
                historyTaskState = currentTaskState;
                currentTaskState = TaskState.Assigned;
                lastAction = action;
                break;
            case Delegate:
                assertState(action, TaskState.Assigned);
                historyTaskState = currentTaskState;
                currentTaskState = TaskState.Delegated;
                lastAction = action;
                break;
            case Suspend:
                if (currentTaskState.isSemiTerminal()) {
                    return false;
                }
                historyTaskState = currentTaskState;
                currentTaskState = TaskState.Suspended;
                lastAction = TaskAction.Suspend;
                break;
            case Terminate:
                historyTaskState = currentTaskState;
                currentTaskState = TaskState.Terminated;
                lastAction = TaskAction.Terminate;
                break;
            case Complete:
                historyTaskState = currentTaskState;
                currentTaskState = TaskState.Completed;
                lastAction = TaskAction.Complete;
                break;
            case Resume:
                assertState(action, TaskState.Suspended);
                TaskState historyState = historyTaskState;
                historyTaskState = currentTaskState;
                currentTaskState = historyState;
                lastAction = TaskAction.Resume;
                break;
            case Create:
                assertState(action, TaskState.Null);
                historyTaskState = currentTaskState;
                currentTaskState = TaskState.Unassigned;
                lastAction = TaskAction.Create;
                break;
            case Null: {
                new Exception("REALLY ??? WHO TRIGGERS THIS???").printStackTrace();
            }
        }
        return true;
    }

    /**
     * Get the current task assignee
     *
     * @return current task assignee
     */
    public String getTaskAssignee() {
        return assignee;
    }

    /**
     * Get the current task state
     *
     * @return current task state [Unassigned, Assigned, Delegated]
     */
    public TaskState getCurrentTaskState() {
        return currentTaskState;
    }

    /**
     * Get the previous task state
     *
     * @return previous task state [Unassigned, Assigned, Delegated]
     */
    public TaskState getPreviousTaskState() {
        return historyTaskState;
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
