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
import com.casefabric.cmmn.actorapi.event.plan.task.TaskEvent;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.humantask.instance.WorkflowTask;
import com.casefabric.json.ValueMap;

import java.io.IOException;

public abstract class HumanTaskEvent extends TaskEvent<HumanTask> {
    /**
     * Constructor used by HumanTaskCreated event, since at that moment the task name is not yet known
     * inside the task actor.
     * @param task
     */
    protected HumanTaskEvent(HumanTask task) {
        super(task);
    }

    protected HumanTaskEvent(ValueMap json) {
        super(json);
    }

    protected void writeHumanTaskEvent(JsonGenerator generator) throws IOException {
        super.writeTaskEvent(generator);
    }

    @Override
    public final void updateState(HumanTask task) {
        updateState(task.getImplementation());
    }

    protected void updateState(WorkflowTask task) {
    }
}
