package org.cafienne.platform;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.serialization.CafienneSerializable;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.UpdateCaseWithPlatformInformation;
import org.cafienne.tenant.akka.command.platform.UpdateTenantWithPlatformInformation;

import java.io.IOException;

public class InformJob implements CafienneSerializable {
    private final BatchJob batch;
    private final ModelCommand command;
    private final String type;
    JobState state = JobState.Pending;
    final String identifier;

    public InformJob(BatchJob batch, ModelCommand command) {
        this.batch = batch;
        this.command = command;
        this.type = command.getClass().getSimpleName();
        this.identifier = command.getMessageId();
    }

    InformJob(BatchJob batch, ValueMap json) {
        this.batch = batch;
        this.type = readField(json, Fields.type);
        this.state = readEnum(json, Fields.state, JobState.class);

        ValueMap commandJson = json.with(Fields.update);
        if ("UpdateTenantWithPlatformInformation".equals(type)) {
            this.command = new UpdateTenantWithPlatformInformation(commandJson);
            this.identifier = command.getMessageId();
        } else if ("UpdateCaseWithPlatformInformation".equals(type)) {
            this.command = new UpdateCaseWithPlatformInformation(commandJson);
            this.identifier = command.getMessageId();
        } else {
            this.command = null;
            this.identifier = "Command less job?!";
        }
    }

    boolean pending() {
        return this.state == JobState.Pending;
    }

    boolean active() {
        return this.state == JobState.Active;
    }

    void run(PlatformService service, JobRunner jobRunner) {
        jobRunner.log("Running job " + this);
        this.state = JobState.Active;
        service.askModel(command, left -> failed(left, jobRunner), right -> succeeded(jobRunner));
    }

    void failed(CommandFailure failure, JobRunner jobRunner) {
        this.state = JobState.Failed;
        batch.reportFailure(this, failure);
        jobRunner.log("Failure while sending command to actor! " + failure.exception());
        jobRunner.log("Releasing handler for next job");
        jobRunner.finished(this);
    }

    void succeeded(JobRunner jobRunner) {
        this.state = JobState.Completed;
        batch.reportSuccess(this);
        jobRunner.finished(this);
    }

    @Override
    public String toString() {
        return "Job to run " + type + " on " + command.getActorId() +" is in state " + state;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.type, type);
        writeField(generator, Fields.state, state);
        writeField(generator, Fields.update, command.toJson());
    }
}
