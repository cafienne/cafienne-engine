/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.definition.CaseDefinitionEvent;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class CaseDefinitionMigrated extends CaseDefinitionEvent {
    public CaseDefinitionMigrated(Case caseInstance, CaseDefinition definition) {
        super(caseInstance, definition);
    }

    public CaseDefinitionMigrated(ValueMap json) {
        super(json);
    }

    public void updateState(Case caseInstance) {
        caseInstance.migrateCaseDefinition(this.definition);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseDefinitionEvent(generator);
    }
}
