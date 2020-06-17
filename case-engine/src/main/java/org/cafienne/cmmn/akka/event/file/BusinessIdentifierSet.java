package org.cafienne.cmmn.akka.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
@Manifest
public class BusinessIdentifierSet extends BusinessIdentifierEvent {
    public final Value value;

    protected enum Fields {
        value
    }

    public BusinessIdentifierSet(CaseFileItem caseFileItem, PropertyDefinition property, Value businessIdentifierValue) {
        super(caseFileItem, property);
        this.value = businessIdentifierValue;
    }

    public BusinessIdentifierSet(ValueMap json) {
        super(json);
        this.value = json.get(Fields.value.toString());
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.value, value);
    }
}
