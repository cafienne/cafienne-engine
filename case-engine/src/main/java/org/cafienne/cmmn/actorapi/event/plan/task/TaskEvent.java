package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class TaskEvent<T extends Task<?>> extends CasePlanEvent<T> {
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
    public void write(JsonGenerator generator) throws IOException {
        writeTaskEvent(generator);
    }
}
