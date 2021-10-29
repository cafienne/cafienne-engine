package org.cafienne.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class BusinessIdentifierEvent extends CaseFileEvent {
    public final String name;
    public final String type;

    protected BusinessIdentifierEvent(CaseFileItem caseFileItem, PropertyDefinition property) {
        super(caseFileItem);
        this.name = property.getName();
        this.type = property.getPropertyType().toString();
    }

    protected BusinessIdentifierEvent(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.type = json.readString(Fields.type);
    }

    @Override
    protected void updateState(CaseFileItem item) {
        item.publishTransition(this);
    }

    public abstract Value<?> getValue();

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "[" + name + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseFileEvent(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.type, type);
    }
}
