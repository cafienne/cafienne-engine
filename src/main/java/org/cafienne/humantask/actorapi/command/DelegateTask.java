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

package org.cafienne.humantask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class DelegateTask extends TaskManagementCommand {
    private final CaseUserIdentity delegate;

    public DelegateTask(CaseUserIdentity user, String caseInstanceId, String taskId, CaseUserIdentity delegate) {
        super(user, caseInstanceId, taskId);
        this.delegate = delegate;
    }

    public DelegateTask(ValueMap json) {
        super(json);
        this.delegate = readUser(json.with(Fields.delegate));
    }

    @Override
    public void validateTaskAction(HumanTask task) {
        verifyTaskPairRestrictions(task, delegate);
        validateCaseTeamMembership(task, delegate);
    }

    protected void validateCaseTeamMembership(HumanTask task, CaseUserIdentity delegate) {
        if (task.getCaseInstance().getCurrentTeamMember().isOwner()) {
            // Case owners will add the team member themselves when assigning/delegating; no need to check membership.
            return;
        }
        // Validate that the new assignee is part of the team
        CaseTeamUser member = task.getCaseInstance().getCaseTeam().getUser(delegate.id());
        if (member == null) {
            raiseException("There is no case team member with id '" + delegate + "'");
        }
    }

    @Override
    public void processWorkflowCommand(WorkflowTask workflowTask) {
        workflowTask.delegate(delegate);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.delegate, delegate);
    }
}
