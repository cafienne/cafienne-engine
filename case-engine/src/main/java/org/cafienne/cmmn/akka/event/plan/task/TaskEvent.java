package org.cafienne.cmmn.akka.event.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class TaskEvent<T extends Task> extends CaseEvent {
    private final static Logger logger = LoggerFactory.getLogger(TaskEvent.class);

    private final String taskId;
    private final String type;

    protected TaskEvent(T task) {
        super(task.getCaseInstance());
        this.taskId = task.getId();
        this.type = task.getType();
    }

    protected TaskEvent(ValueMap json) {
        super(json);
        this.taskId = json.raw(Fields.taskId);
        this.type = json.raw(Fields.type);
    }

    protected T getTask() {
        T task = actor.getPlanItemById(getTaskId());
        if (task == null) {
            logger.error("MAJOR ERROR: Cannot recover task event for task with id " + getTaskId() + ", because the plan item cannot be found");
        }
        return task;
    }

    /**
     * Returns type of task, taken from plan item. Typically HumanTask, ProcessTask or CaseTask.
     * @return
     */
    public String getType() {
        return this.type;
    }

    public String getTaskId() {
        return taskId;
    }

    public void writeTaskEvent(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.taskId, taskId);
        writeField(generator, Fields.type, type);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeTaskEvent(generator);
    }
}
