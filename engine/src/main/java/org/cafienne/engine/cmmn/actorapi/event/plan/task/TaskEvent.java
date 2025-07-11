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

package org.cafienne.engine.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.engine.cmmn.actorapi.event.plan.CasePlanEvent;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.engine.cmmn.instance.Task;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class TaskEvent<T extends Task<?>> extends CasePlanEvent {

    protected TaskEvent(T task) {
        super(task);
    }

    protected TaskEvent(ValueMap json) {
        super(json);
    }

    /**
     * Get the task id
     * @return id of the task
     */
    public String getTaskId() {
        return getPlanItemId();
    }

    public void writeTaskEvent(JsonGenerator generator) throws IOException {
        super.writeCasePlanEvent(generator);
    }

    @Override
    public void updatePlanItemState(PlanItem<?> item) {
        updateState((T) item);
    }

    public abstract void updateState(T task);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeTaskEvent(generator);
    }

    @Override
    public String getPlanItemId() {
        // Unfortunately need to override this, because recovery uses the plan item id,
        // and older versions of TaskEvent did not invoke parent's serializer.
        String planItemId = super.getPlanItemId();
        if (planItemId == null) {
            return rawJson().readString(Fields.taskId);
        } else {
            return planItemId;
        }
    }

    /**
     * Get the name of the task
     * @return
     */
    public String getTaskName() {
        return path.isEmpty() ? rawJson().readString(Fields.taskName) : path.name;
    }
}
