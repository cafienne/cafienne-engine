package org.cafienne.cmmn.actorapi.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class TaskEvent<T extends Task<?>> extends CasePlanEvent {
    public final String taskId; // taskName is same as the planItem id
    private final String taskName; // taskName is same as the planItemName

    protected TaskEvent(T task) {
        super(task);
        this.taskName = task.getName();
        this.taskId = task.getId();
    }

    protected TaskEvent(ValueMap json) {
        super(json);
        this.taskName = json.readString(Fields.taskName);
        // Not all old style task event carry task id, some have plan item id,
        //  so taking that as default value if task id is missing.
        //  Note: plan item id is parsed from json in the super constructor,
        //  and available through the super getter (which is overridden below, so make sure to call super itself).
        this.taskId = json.readString(Fields.taskId, super.getPlanItemId());
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
        writeField(generator, Fields.taskName, taskName);
        writeField(generator, Fields.taskId, taskId);
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
        return taskId;
    }

    /**
     * Get the name of the task
     * @return
     */
    public String getTaskName() {
        return taskName;
    }
}
