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
