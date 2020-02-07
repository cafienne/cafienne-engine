/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskState;

import java.util.Arrays;

abstract class WorkflowCommand extends HumanTaskCommand {
    protected WorkflowCommand(TenantUser tenantUser, String caseInstanceId, String taskId) {
        super(tenantUser, caseInstanceId, taskId);
    }

    @Deprecated
    protected WorkflowCommand(TenantUser tenantUser, String taskId) {
        this(tenantUser, null, taskId);
    }

    protected WorkflowCommand(ValueMap json) {
        super(json);
    }

    /**
     * Helper method that validates whether a task is in one of the expected states.
     * @param task
     * @param expectedStates*/
    protected void validateState(HumanTask task, TaskState... expectedStates) {
        TaskState currentTaskState = task.getImplementation().getCurrentTaskState();
        for (int i = 0; i < expectedStates.length; i++) {
            if (expectedStates[i].equals(currentTaskState)) {
                return;
            }
        }
        String msg = this.getClass().getSimpleName() + " cannot be done because task (" + getTaskId() + ") is in " + currentTaskState + " state, but should be in any of " + Arrays.asList(expectedStates) + " state";
        throw new InvalidCommandException(msg);
    }
}
