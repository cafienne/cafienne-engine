package org.cafienne.cmmn.instance.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.CafienneSerializable;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.ValueMap;

import java.io.IOException;

public class MigrationScript implements CafienneSerializable {
    private final String source;

    public MigrationScript(String source) {
        this.source = source;
    }

    public MigrationScript(ValueMap json) {
        this.source = readField(json, Fields.source);
    }

    @Override
    public String toString() {
        return "MigrationScript[" + source + ']';
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        writeField(generator, Fields.source, source);
        generator.writeEndObject();
    }
}
