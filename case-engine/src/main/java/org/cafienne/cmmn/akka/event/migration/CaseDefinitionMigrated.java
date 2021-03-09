/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.event.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CafienneVersion;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.migration.MigrationScript;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class CaseDefinitionMigrated extends CaseEvent {
    private final CaseDefinition definition;
    private final MigrationScript migrationScript;

    public CaseDefinitionMigrated(Case caseInstance, CaseDefinition definition, MigrationScript migrationScript) {
        super(caseInstance);
        this.definition = definition;
        this.migrationScript = migrationScript;
    }

    public CaseDefinitionMigrated(ValueMap json) {
        super(json);
        this.definition = readDefinition(json, Fields.definition, CaseDefinition.class);
        this.migrationScript = new MigrationScript(json.with(Fields.script));
    }

    /**
     * Returns the case definition that was applied to the case instance
     * @return
     */
    public CaseDefinition getDefinition() {
        return this.definition;
    }

    public void updateState(Case caseInstance) {
        caseInstance.migrateCaseDefinition(this.definition, this.migrationScript);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.definition, definition);
        writeField(generator, Fields.script, migrationScript);
    }
}
