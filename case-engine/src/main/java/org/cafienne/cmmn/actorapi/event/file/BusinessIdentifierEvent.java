package org.cafienne.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.Value;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.instance.casefile.InvalidPathException;
import org.cafienne.cmmn.instance.casefile.Path;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class BusinessIdentifierEvent extends CaseEvent {
    public final Path path;
    public final String name;
    public final String type;

    protected BusinessIdentifierEvent(CaseFileItem caseFileItem, PropertyDefinition property) {
        super(caseFileItem.getCaseInstance());
        this.path = caseFileItem.getPath();
        this.name = property.getName();
        this.type = property.getPropertyType().toString();
    }

    protected BusinessIdentifierEvent(ValueMap json) {
        super(json);
        this.path = readPath(json, Fields.path);
        this.name = readField(json, Fields.name);
        this.type = readField(json, Fields.type);
    }

    @Override
    public void updateState(Case caseInstance) {
        try {
            // Resolve the path on the case file
            // Have to recover it this way in order to overcome fact that Path.definition is not serializable
            CaseFileItem item = path.resolve(caseInstance);
            item.publishTransition(this);
        } catch (InvalidPathException shouldNotHappen) {
            logger.error("Could not recover path on case instance?!", shouldNotHappen);
        }
    }

    public abstract Value<?> getValue();

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
