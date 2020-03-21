/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.task.process;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ProcessTaskDefinition;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.*;

public class ProcessTask extends Task<ProcessTaskDefinition> {
    public ProcessTask(String id, int index, ItemDefinition itemDefinition, ProcessTaskDefinition definition, Stage stage) {
        super(id, index, itemDefinition, definition, stage);
    }

    @Override
    protected void startImplementation(ValueMap inputParameters) {
        final String taskId = this.getId();
        final TenantUser user = getCaseInstance().getCurrentUser();
        final String taskName = this.getName();
        final String rootActorId = this.getCaseInstance().getRootActorId();
        final String parentId = this.getCaseInstance().getId();
        final boolean debugMode = this.getCaseInstance().debugMode();

        getCaseInstance().askProcess(new StartProcess(user, taskId, taskName, getDefinition().getImplementationDefinition(), inputParameters, parentId, rootActorId, debugMode),
                left -> goFault(new ValueMap("exception", left.toJson())),
                right -> {
                    if (!this.getDefinition().isBlocking()) {
                        goComplete(new ValueMap());
                    }
                });
    }

    @Override
    protected void createInstance() {
    }

    @Override
    protected void suspendInstance() {
        tell(new SuspendProcess(getCaseInstance().getCurrentUser(), getId()));
    }

    @Override
    protected void resumeInstance() {
        tell(new ResumeProcess(getCaseInstance().getCurrentUser(), getId()));
    }

    @Override
    protected void terminateInstance() {
        if (getHistoryState() == State.Available) {
            addDebugInfo(() -> "Terminating process task '" + getName() + "' without it being started; no need to inform the task actor");
        } else {
            tell(new TerminateProcess(getCaseInstance().getCurrentUser(), getId()));
        }
    }

    @Override
    protected void reactivateImplementation(ValueMap inputParameters) {
        // Apparently process has failed so we can trying again
        tell(new ReactivateProcess(getCaseInstance().getCurrentUser(), getId(), inputParameters));
    }

    private void tell(ProcessCommand command) {
        if (!this.getDefinition().isBlocking()) {
            return;
        }
        getCaseInstance().askProcess(command, left -> goFault(new ValueMap("exception", left.toJson())));
    }
}
