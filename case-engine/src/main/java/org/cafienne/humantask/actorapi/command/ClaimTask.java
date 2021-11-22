/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.command;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.actorapi.response.HumanTaskResponse;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ClaimTask extends WorkflowCommand {
    public ClaimTask(CaseUserIdentity user, String caseInstanceId, String taskId) {
        super(user, caseInstanceId, taskId);
    }

    public ClaimTask(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(HumanTask task) {
        super.validateProperCaseRole(task);
        super.validateState(task, TaskState.Unassigned);
    }

    @Override
    public HumanTaskResponse process(WorkflowTask workflowTask) {
        workflowTask.claim(this.getUser().id());
        return new HumanTaskResponse(this);
    }
}
