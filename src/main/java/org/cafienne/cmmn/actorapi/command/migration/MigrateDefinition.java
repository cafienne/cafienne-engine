/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.actorapi.response.migration.MigrationStartedResponse;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class MigrateDefinition extends CaseCommand {
    private final CaseDefinition newDefinition;

    /**
     * Migrate the definition of a case.
     *
     * @param caseInstanceId The instance identifier of the case
     * @param newDefinition  The case definition (according to the CMMN xsd) to be updated to
     */
    public MigrateDefinition(CaseUserIdentity user, String caseInstanceId, CaseDefinition newDefinition) {
        super(user, caseInstanceId);
        this.newDefinition = newDefinition;
    }

    public MigrateDefinition(ValueMap json) {
        super(json);
        this.newDefinition = json.readDefinition(Fields.definition, CaseDefinition.class);
    }

    @Override
    public String toString() {
        return "Migrate Case Definition '" + newDefinition.getName() + "'";
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        if (newDefinition.getDefinitionsDocument().equals(caseInstance.getDefinition().getDefinitionsDocument())) {
            // ??? why again???? ;)
            caseInstance.addDebugInfo(() -> "No need to migrate definition of case " + caseInstance.getId() + " (proposed definition already in use by the case instance)");
        } else {
            caseInstance.migrate(newDefinition);
        }
        return new MigrationStartedResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.definition, newDefinition);
    }
}
