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

package org.cafienne.cmmn.instance.task.process;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.*;

class ProcessTaskActorInformer extends ProcessInformer {

    public ProcessTaskActorInformer(ProcessTask task) {
        super(task);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        final String taskId = task.getId();
        final CaseUserIdentity user = getCaseInstance().getCurrentUser();
        final String tenant = getCaseInstance().getTenant();
        final String taskName = task.getName();
        final String rootActorId = task.getCaseInstance().getRootActorId();
        final String parentId = task.getCaseInstance().getId();
        final boolean debugMode = task.getCaseInstance().debugMode();
        final StartProcess command = new StartProcess(user, tenant, taskId, taskName, task.getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode);

        task.startTaskImplementation(command);
    }

    @Override
    protected void suspendImplementation() {
        task.tellTaskImplementation(new SuspendProcess(getCaseInstance().getCurrentUser(), task.getId(), getCaseInstance().getRootCaseId()));
    }

    @Override
    protected void resumeImplementation() {
        task.tellTaskImplementation(new ResumeProcess(getCaseInstance().getCurrentUser(), task.getId(), getCaseInstance().getRootCaseId()));
    }

    @Override
    protected void terminateImplementation() {
        if (task.getHistoryState() == State.Available) {
            getCaseInstance().addDebugInfo(() -> "Terminating process task '" + task.getName() + "' without it being started; no need to inform the task actor");
        } else {
            task.tellTaskImplementation(new TerminateProcess(getCaseInstance().getCurrentUser(), task.getId(), getCaseInstance().getRootCaseId()));
        }
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        final String taskId = task.getId();
        final CaseUserIdentity user = getCaseInstance().getCurrentUser();
        final String tenant = getCaseInstance().getTenant();
        final String taskName = task.getName();
        final String rootActorId = task.getCaseInstance().getRootActorId();
        final String parentId = task.getCaseInstance().getId();
        final boolean debugMode = task.getCaseInstance().debugMode();
        task.tellTaskImplementation(new ReactivateProcess(user, tenant, taskId, taskName, task.getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode));
    }

    @Override
    protected void migrateDefinition(ProcessTaskDefinition newDefinition) {
        task.giveNewDefinition(new MigrateProcessDefinition(getCaseInstance().getCurrentUser(), task.getId(), task.getCaseInstance().getRootActorId(),  newDefinition.getImplementationDefinition()));
    }
}
