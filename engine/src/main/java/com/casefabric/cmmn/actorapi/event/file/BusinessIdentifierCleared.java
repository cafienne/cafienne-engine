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

package com.casefabric.cmmn.actorapi.event.file;

import com.casefabric.cmmn.definition.casefile.PropertyDefinition;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

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
    public Value<?> getValue() {
        return null;
    }
}
