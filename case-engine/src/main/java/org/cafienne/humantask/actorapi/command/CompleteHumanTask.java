/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.command;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.json.ValueMap;
import org.cafienne.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.humantask.instance.WorkflowTask;

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
    public CompleteHumanTask(TenantUser tenantUser, String caseInstanceId, String taskId, ValueMap taskOutput) {
        super(tenantUser, caseInstanceId, taskId, taskOutput);
    }

    public CompleteHumanTask(ValueMap json) {
        super(json);
    }

    @Override
    public HumanTaskResponse process(WorkflowTask workflowTask) {
        workflowTask.complete(taskOutput);
        return new HumanTaskResponse(this);
    }

    @Override
    public String toString() {
        String taskName = task != null ? task.getName() + " with id " + getTaskId() : getTaskId() + " (unknown name)";
        return "Complete HumanTask '" + taskName + "' with output\n" + taskOutput;
    }
}
