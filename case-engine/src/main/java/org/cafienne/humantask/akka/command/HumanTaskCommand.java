package org.cafienne.humantask.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;

import java.io.IOException;

public abstract class HumanTaskCommand extends CaseCommand {
    private final String taskId;
    private HumanTask task;

    private enum Fields {
        taskId
    }

    protected HumanTaskCommand(TenantUser tenantUser, String caseInstanceId, String taskId) {
        super(tenantUser, caseInstanceId);
        if (taskId == null || taskId.trim().isEmpty()) {
            throw new NullPointerException("Task id should not be null or empty");
        }

        this.taskId = taskId;
    }

    protected HumanTaskCommand(ValueMap json) {
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
        if (!(planItem.getInstance() instanceof HumanTask)) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + ": The plan item with id " + planItem.getId() + " in case " + caseInstance.getId() + " is not a HumanTask");
        }
        // Good. It's a HumanTask
        task = planItem.getInstance();

        // TODO: validate that this is a proper check (e.g. why could i not Delegate a suspended or failed task??)
        State currentState = task.getPlanItem().getState();
        if (currentState != State.Active) {
            throw new InvalidCommandException(this.getClass().getSimpleName() + " cannot be done because task " + planItem.getName() + " (" + taskId + ") is not in Active but in " + currentState + " state");
        }

        // Now have the actual command do its own validation on the task
        validate(task);
    }

    public abstract void validate(HumanTask task) throws InvalidCommandException;

    public ModelResponse process(Case caseInstance) {
        return process(task);

    }

    public abstract ModelResponse process(HumanTask actor) ;

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
    }
}
