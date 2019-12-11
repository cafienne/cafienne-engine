package org.cafienne.humantask.akka.command.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.humantask.akka.command.HumanTaskCommand;

import java.io.IOException;

@Manifest
public class HumanTaskValidationResponse extends HumanTaskResponse {
    private final ValueMap value;

    private enum Fields {
        value
    }

    public HumanTaskValidationResponse(HumanTaskCommand command, ValueMap value) {
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
