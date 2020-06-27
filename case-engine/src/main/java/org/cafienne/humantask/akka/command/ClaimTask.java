/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskClaimed;
import org.cafienne.humantask.akka.event.HumanTaskOwnerChanged;
import org.cafienne.humantask.instance.TaskState;

@Manifest
public class ClaimTask extends HumanTaskCommand {
    public ClaimTask(TenantUser tenantUser, String caseInstanceId, String taskId) {
        super(tenantUser, caseInstanceId, taskId);
    }

    public ClaimTask(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(HumanTask task) {
        super.validateState(task, TaskState.Unassigned);
        super.validateProperCaseRole(task);
    }

    @Override
    public HumanTaskResponse process(HumanTask task) {
        String claimer = this.getUser().id();
        task.addEvent(new HumanTaskClaimed(task, claimer));
        task.addEvent(new HumanTaskOwnerChanged(task, claimer));
        return new HumanTaskResponse(this);
    }
}
