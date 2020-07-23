package org.cafienne.humantask.akka.command.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.humantask.akka.command.WorkflowCommand;

import java.io.IOException;

@Manifest
public class HumanTaskResponse extends CaseResponse {
    private final String taskId;

    public HumanTaskResponse(WorkflowCommand command) {
        super(command);
        this.taskId = command.actorId;
    }

    public HumanTaskResponse(ValueMap json) {
        super(json);
        this.taskId = readField(json, Fields.taskId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
    }
}
