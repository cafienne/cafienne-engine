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

package com.casefabric.cmmn.instance.task.process;

import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.definition.ProcessTaskDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.json.ValueMap;
import com.casefabric.processtask.actorapi.command.*;

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
        task.tellTaskImplementation(new SuspendProcess(getCaseInstance().getCurrentUser(), task.getId()));
    }

    @Override
    protected void resumeImplementation() {
        task.tellTaskImplementation(new ResumeProcess(getCaseInstance().getCurrentUser(), task.getId()));
    }

    @Override
    protected void terminateImplementation() {
        if (task.getHistoryState() == State.Available) {
            getCaseInstance().addDebugInfo(() -> "Terminating process task '" + task.getName() + "' without it being started; no need to inform the task actor");
        } else {
            task.tellTaskImplementation(new TerminateProcess(getCaseInstance().getCurrentUser(), task.getId()));
        }
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // TODO: reactivate is invoked after actual state has become active again,
        //  and that means that task.getImplementationState.getStarted returns the wrong value
        // NOT SURE WHAT THE IMPACT OF SUCH A SCENARIO is
        if (task.getImplementationState().isStarted()) {
            // Apparently process has failed so we can trying again
            task.tellTaskImplementation(new ReactivateProcess(getCaseInstance().getCurrentUser(), task.getId(), inputParameters));
        } else {
            startImplementation(inputParameters);
        }
    }

    @Override
    protected void migrateDefinition(ProcessTaskDefinition newDefinition) {
        task.giveNewDefinition(new MigrateProcessDefinition(getCaseInstance().getCurrentUser(), task.getId(), newDefinition.getImplementationDefinition()));
    }
}
