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

package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.actorapi.event.plan.task.TaskCommandRejected;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskImplementationNotStarted;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskImplementationReactivated;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskImplementationStarted;

public class TaskImplementationActorState {
    private final Task<?> task;
    private boolean isStarted = false;
    private boolean foundFailure = false;

    TaskImplementationActorState(Task<?> task) {
        this.task = task;
    }

    public boolean isStarted() {
        // For backwards compatibility:
        //  If a task is successfully started in older versions of Cafienne, it will be in state Active;
        //  However, the state of the task interaction is not stored through below handle events.
        //  Therefore, we use the flags in combination with the known task state.
        //  - if (isStated == true), then we have received and stored an event for it.
        //  - if (foundFailure == true), then we have received and stored an event for it.
        //  - if (foundFailure == false), we did not receive events, and then we rely on state inside the case instead of the implementation.
        return isStarted || (!foundFailure && task.getState().isAlive());
    }

    void updateState(TaskImplementationStarted event) {
        task.addDebugInfo(() -> "Task " + task + " confirmed a successful start.");
        isStarted = true;
    }

    void updateState(TaskImplementationNotStarted event) {
        task.addDebugInfo(() -> "Task " + task + " could not be started.");
        isStarted = false;
        updateState((TaskCommandRejected) event);
    }

    void updateState(TaskImplementationReactivated event) {
        task.addDebugInfo(() -> "Task " + task + " confirmed a successful reactivate.");
        isStarted = true;
    }

    void updateState(TaskCommandRejected event) {
        task.addDebugInfo(() -> "Task " + task + " reported a command rejection.", event.rawJson());
        foundFailure = true;
    }

    @Override
    public String toString() {
        return "TaskImplementationActorState{" +
                "task=" + task +
                ", task.state=" + task.getState() +
                ", isStarted=" + isStarted +
                ", foundFailure=" + foundFailure +
                ", isStarted()=" + isStarted() +
                '}';
    }
}
