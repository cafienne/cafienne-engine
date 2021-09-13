package org.cafienne.cmmn.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public class CaseResponseWithValueMap extends CaseResponse {
    private final ValueMap value;

    protected CaseResponseWithValueMap(CaseCommand command, ValueMap value) {
        super(command);
        this.value = value;
    }

    protected CaseResponseWithValueMap(ValueMap json) {
        super(json);
        this.value = readMap(json, Fields.response);
    }

    /**
     * Returns a JSON representation of this object
     * @return
     */
    @Override
    public ValueMap toJson() {
        return value;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.response, value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + toJson();
    }
}
