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
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class AssignTask extends TaskManagementCommand {
    protected final CaseUserIdentity assignee;

    public AssignTask(CaseUserIdentity user, String caseInstanceId, String taskId, CaseUserIdentity assignee) {
        super(user, caseInstanceId, taskId);
        this.assignee = assignee;
    }

    public AssignTask(ValueMap json) {
        super(json);
        this.assignee = readUser(json.with(Fields.assignee));
    }

    @Override
    public void validateTaskAction(HumanTask task) {
        verifyTaskPairRestrictions(task, assignee);
        verifyCaseTeamMembershipOfAssignee(task, assignee);
    }

    private void verifyCaseTeamMembershipOfAssignee(HumanTask task, CaseUserIdentity assignee) {
        // Case owners can always assign tasks to others, and also add them to the case team with the appropriate role
        if (task.getCaseInstance().getCurrentTeamMember().isOwner()) {
            // Case owners will add the team member themselves when assigning/delegating; no need to check membership.
            return;
        }

        // Validate that the new assignee is part of the team and has the proper role
        CaseTeamUser member = task.getCaseInstance().getCaseTeam().getUser(assignee.id());
        if (member == null) {
            raiseException("There is no case team member with id '" + assignee + "'");
        } else {
            // Validate that - if the task needs a role - the new assignee has that role
            CaseRoleDefinition role = task.getPerformer();
            if (role != null) {
                // Members need to have the role, Owners don't need to
                if (!member.isOwner() && !member.getCaseRoles().contains(role.getName())) {
                    raiseAuthorizationException("The case team member with id '" + assignee + "' does not have the case role " + role.getName());
                }
            }
        }
    }

    @Override
    public void processWorkflowCommand(WorkflowTask workflowTask) {
        workflowTask.assign(this.assignee);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
