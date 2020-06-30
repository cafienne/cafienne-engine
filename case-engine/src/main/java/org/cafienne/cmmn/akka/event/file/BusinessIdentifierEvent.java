package org.cafienne.cmmn.akka.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class BusinessIdentifierEvent extends CaseEvent {
    public final String path;
    public final String name;
    public final String type;

    protected enum Fields {
        path, name, value, type
    }

    protected BusinessIdentifierEvent(CaseFileItem caseFileItem, PropertyDefinition property) {
        super(caseFileItem.getCaseInstance());
        this.path = caseFileItem.getPath().toString();
        this.name = property.getName();
        this.type = property.getPropertyType().toString();
    }

    protected BusinessIdentifierEvent(ValueMap json) {
        super(json);
        this.path = readField(json, Fields.path);
        this.name = readField(json, Fields.name);
        this.type = readField(json, Fields.type);
    }

    @Override
    public String getDescription() {
        return this.getClass().getSimpleName() + "[" + name + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.path, path);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.type, type);
    }
}
