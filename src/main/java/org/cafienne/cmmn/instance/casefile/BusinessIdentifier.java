/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    protected void lostDefinition() {
        // Ehm ... introduce a special event for this???
        // but ... we also must check whether a business identifier with the same name now exists
        //  somewhere else in the case file???
        //
        // For now simply clear the identifier
        clear();
    }
}
