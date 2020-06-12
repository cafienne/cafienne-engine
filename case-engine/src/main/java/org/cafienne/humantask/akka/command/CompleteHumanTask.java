/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.validation.ValidationError;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskCompleted;
import org.cafienne.humantask.instance.TaskState;

import java.io.IOException;

/**
 * This command must be used to complete a human task with additional task output parameters.
 */
@Manifest
public class CompleteHumanTask extends HumanTaskCommand {
    protected final ValueMap taskOutput;
    protected Task<?> task;

    private enum Fields {
        taskOutput
    }

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
    public CompleteHumanTask(TenantUser tenantUser, String caseInstanceId, String taskId, ValueMap taskOutput) {
        super(tenantUser, caseInstanceId, taskId);
        this.taskOutput = taskOutput;
    }

    public CompleteHumanTask(ValueMap json) {
        super(json);
        this.taskOutput = readMap(json, Fields.taskOutput);
    }

    @Override
    public void validate(HumanTask task) throws InvalidCommandException {
        super.validateTaskOwnership(task);
        super.validateState(task, TaskState.Assigned, TaskState.Delegated);
    }

    public ValueMap getTaskOutput() {
        return taskOutput;
    }

    @Override
    public HumanTaskResponse process(HumanTask task) {
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


        task.addEvent(new HumanTaskCompleted(task, this.taskOutput));
        // This will generate PlanItemTransitioned and CaseFileItem events
        task.goComplete(this.taskOutput);
        return new HumanTaskResponse(this);
    }

    @Override
    public String toString() {
        String taskName = task != null ? task.getName() + " with id " + getTaskId() : getTaskId() + " (unknown name)";
        return "Complete HumanTask '" + taskName + "' with output\n" + taskOutput;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskOutput, taskOutput);
    }
}
