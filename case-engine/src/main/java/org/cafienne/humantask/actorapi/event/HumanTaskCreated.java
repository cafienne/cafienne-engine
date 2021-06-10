package org.cafienne.humantask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class HumanTaskCreated extends HumanTaskEvent {
    private final Instant createdOn;
    private final String createdBy;

    @Deprecated
    public HumanTaskCreated(HumanTask task) {
        super(task);
        this.createdOn = task.getCaseInstance().getTransactionTimestamp();
        this.createdBy = task.getCaseInstance().getCurrentUser().id();
        throw new IllegalArgumentException("This code is no longer in use");
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
