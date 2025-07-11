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

package org.cafienne.engine.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.engine.humantask.instance.TaskAction;
import org.cafienne.engine.humantask.instance.TaskState;
import org.cafienne.engine.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class HumanTaskAssigned extends HumanTaskTransitioned {
    /**
     * New assignee of the task
     */
    public final String assignee; // assignee of the task

    public HumanTaskAssigned(HumanTask task, String assignee) {
        this(task, assignee, TaskState.Assigned, TaskAction.Assign);
    }

    protected HumanTaskAssigned(HumanTask task, String assignee, TaskState nextState, TaskAction transition) {
        super(task, nextState, transition);
        this.assignee = assignee;
    }

    public HumanTaskAssigned(ValueMap json) {
        super(json);
        this.assignee = json.readString(Fields.assignee);
    }

    @Override
    public void updateState(WorkflowTask task) {
        super.updateState(task);
        task.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTransitionEvent(generator);
        writeField(generator, Fields.assignee, assignee);
    }
}
