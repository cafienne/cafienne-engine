package org.cafienne.cmmn.akka.event.file;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.akka.actor.serialization.json.ValueMap;

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
