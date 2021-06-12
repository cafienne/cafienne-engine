package org.cafienne.humantask.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.json.ValueMap;
import org.cafienne.humantask.actorapi.command.WorkflowCommand;

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