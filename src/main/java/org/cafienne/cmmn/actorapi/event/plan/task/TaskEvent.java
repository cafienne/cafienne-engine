package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
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
