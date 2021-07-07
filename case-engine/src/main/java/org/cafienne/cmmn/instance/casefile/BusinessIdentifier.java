package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.actorapi.event.file.BusinessIdentifierCleared;
import org.cafienne.cmmn.actorapi.event.file.BusinessIdentifierEvent;
import org.cafienne.cmmn.actorapi.event.file.BusinessIdentifierSet;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

class BusinessIdentifier extends CMMNElement<PropertyDefinition> {
    private final CaseFileItem item;
    private Value<?> currentValue = null;

    BusinessIdentifier(CaseFileItem item, PropertyDefinition property) {
        super(item, property);
        this.item = item;
    }

    void clear() {
        item.getCaseInstance().addEvent(new BusinessIdentifierCleared(item, getDefinition()));
    }

    void update(ValueMap map) {
        if (map.has(getDefinition().getName())) {
            Value<?> potentialNewValue = map.get(getDefinition().getName());
            if (! potentialNewValue.equals(currentValue)) {
                item.getCaseInstance().addEvent(new BusinessIdentifierSet(item, getDefinition(), potentialNewValue));
            }
        }
    }

    void updateState(BusinessIdentifierEvent event) {
        this.currentValue = event.getValue();
    }
}
