/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.definition;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.CaseBaseEvent;
import org.cafienne.cmmn.actorapi.event.CaseEvent;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

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
        this.definition = readDefinition(json, Fields.definition, CaseDefinition.class);
        this.caseName = readField(json, Fields.caseName);
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
