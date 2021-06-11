package org.cafienne.cmmn.actorapi.event.file;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.ValueMap;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
@Manifest
public class BusinessIdentifierCleared extends BusinessIdentifierEvent {
    public BusinessIdentifierCleared(CaseFileItem caseFileItem, PropertyDefinition property) {
        super(caseFileItem, property);
    }

    public BusinessIdentifierCleared(ValueMap json) {
        super(json);
    }

    @Override
    public Value getValue() {
        return null;
    }
}
