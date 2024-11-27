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

package org.cafienne.cmmn.actorapi.event.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
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
