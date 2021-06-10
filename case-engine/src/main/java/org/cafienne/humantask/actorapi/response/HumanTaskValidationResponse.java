package org.cafienne.humantask.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.serialization.Fields;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.humantask.actorapi.command.WorkflowCommand;

import java.io.IOException;

@Manifest
public class HumanTaskValidationResponse extends HumanTaskResponse {
    private final ValueMap value;

    public HumanTaskValidationResponse(WorkflowCommand command, ValueMap value) {
        super(command);
        this.value = value;
    }

    public HumanTaskValidationResponse(ValueMap json) {
        super(json);
        this.value = readMap(json, Fields.value);
    }

    public ValueMap value() {
        return value;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.value, value);
    }
}
