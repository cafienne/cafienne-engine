package org.cafienne.humantask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class HumanTaskCreated extends HumanTaskEvent {
    private final Instant createdOn;
    private final String createdBy;

    private enum Fields {
        createdOn, createdBy
    }

    public HumanTaskCreated(HumanTask task) {
        super(task);
        this.createdOn = task.getCaseInstance().getTransactionTimestamp();
        this.createdBy = task.getCaseInstance().getCurrentUser().id();
    }

    public HumanTaskCreated(ValueMap json) {
        super(json);
        this.createdOn = readInstant(json, Fields.createdOn);
        this.createdBy = readField(json, Fields.createdBy);
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return "HumanTask[" + getTaskId() + "] is active";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeHumanTaskEvent(generator);
        writeField(generator, Fields.createdOn, createdOn);
        writeField(generator, Fields.createdBy, createdBy);
    }
}
