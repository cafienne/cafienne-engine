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

package com.casefabric.cmmn.actorapi.event.definition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.actorapi.event.CaseBaseEvent;
import com.casefabric.cmmn.actorapi.event.CaseEvent;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.ValueMap;

import java.io.IOException;

public abstract class CaseDefinitionEvent extends CaseBaseEvent {
    protected final CaseDefinition definition;
    protected final String caseName;

    protected CaseDefinitionEvent(Case caseInstance, CaseDefinition definition) {
        super(caseInstance);
        this.definition = definition;
        this.caseName = definition.getName();
    }

    protected CaseDefinitionEvent(ValueMap json) {
        super(json);
        this.definition = json.readDefinition(Fields.definition, CaseDefinition.class);
        this.caseName = json.readString(Fields.caseName);
    }

    /**
     * Returns the name of the case definition
     *
     * @return
     */
    public final String getCaseName() {
        return caseName;
    }

    /**
     * Returns the case definition that was applied to the case instance
     * @return
     */
    public CaseDefinition getDefinition() {
        return this.definition;
    }

    protected void writeCaseDefinitionEvent(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.caseName, caseName);
        writeField(generator, Fields.definition, definition);
    }
}
