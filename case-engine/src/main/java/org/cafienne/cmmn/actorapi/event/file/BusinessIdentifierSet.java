package org.cafienne.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
@Manifest
public class BusinessIdentifierSet extends BusinessIdentifierEvent {
    public final Value<?> value;

    public BusinessIdentifierSet(CaseFileItem caseFileItem, PropertyDefinition property, Value<?> businessIdentifierValue) {
        super(caseFileItem, property);
        this.value = businessIdentifierValue;
    }

    public BusinessIdentifierSet(ValueMap json) {
        super(json);
        this.value = json.get(Fields.value);
    }

    @Override
    public Value<?> getValue() {
        return value;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.value, value);
    }
}
