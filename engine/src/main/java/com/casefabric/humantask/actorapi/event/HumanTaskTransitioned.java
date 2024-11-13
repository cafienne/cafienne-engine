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

package com.casefabric.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.humantask.instance.TaskAction;
import com.casefabric.humantask.instance.TaskState;
import com.casefabric.humantask.instance.WorkflowTask;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;

public abstract class HumanTaskTransitioned extends HumanTaskEvent {
    private final TaskState currentState; // current taskState [Unassigned, Assigned or Delegated]
    private final TaskState historyState; // previous taskState [Unassigned, Assigned or Delegated]
    private final TaskAction transition; // last action happened on the task

    protected HumanTaskTransitioned(HumanTask task, TaskState currentState, TaskState historyState, TaskAction transition) {
        super(task);
        this.currentState = currentState;
        this.historyState = historyState;
        this.transition = transition;
    }

    protected HumanTaskTransitioned(HumanTask task, TaskState currentState, TaskAction transition) {
        this(task, currentState, task.getImplementation().getCurrentState(), transition);
    }

    protected HumanTaskTransitioned(ValueMap json) {
        super(json);
        this.currentState = json.readEnum(Fields.currentState, TaskState.class);
        this.historyState = json.readEnum(Fields.historyState, TaskState.class);
        this.transition = json.readEnum(Fields.transition, TaskAction.class);
    }

    public void writeTransitionEvent(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.historyState, historyState);
        writeField(generator, Fields.transition, transition);
        writeField(generator, Fields.currentState, currentState);
    }

    @Override
    public String toString() {
        return "Task " + getTaskName() + "[" + getTaskId() + "]." + getTransition() + ", causing transition from " + getHistoryState() + " to " + getCurrentState();
    }

    @Override
    public void updateState(WorkflowTask task) {
        task.updateState(this);
    }

    /**
     * Get the current task state
     *
     * @return current task state
     */
    public TaskState getCurrentState() {
        return currentState;
    }

    /**
     * Get the previous task state
     *
     * @return previous task state
     */
    public TaskState getHistoryState() {
        return historyState;
    }

    /**
     * Get the last action happened on task
     *
     * @return last action happened on task
     */
    public TaskAction getTransition() {
        return transition;
    }
}
