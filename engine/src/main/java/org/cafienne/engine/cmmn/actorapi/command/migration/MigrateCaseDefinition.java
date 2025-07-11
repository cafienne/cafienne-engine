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

package org.cafienne.engine.cmmn.actorapi.command.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.cmmn.actorapi.command.CaseCommand;
import org.cafienne.engine.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.engine.cmmn.actorapi.response.migration.MigrationStartedResponse;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.team.CaseTeamError;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class MigrateCaseDefinition extends CaseCommand {
    private final CaseDefinition newDefinition;
    private final CaseTeam newCaseTeam;

    /**
     * Migrate the definition of a case.
     *
     * @param caseInstanceId The instance identifier of the case
     * @param newDefinition  The case definition (according to the CMMN xsd) to be updated to
     */
    public MigrateCaseDefinition(CaseUserIdentity user, String caseInstanceId, CaseDefinition newDefinition, CaseTeam newTeam) {
        super(user, caseInstanceId);
        this.newDefinition = newDefinition;
        this.newCaseTeam = newTeam;
    }

    public MigrateCaseDefinition(ValueMap json) {
        super(json);
        this.newDefinition = json.readDefinition(Fields.definition, CaseDefinition.class);
        this.newCaseTeam = json.readObject(Fields.team, CaseTeam::deserialize);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        if (newCaseTeam != null) {
            if (newCaseTeam.owners().isEmpty()) {
                throw new CaseTeamError("The case team needs to have at least one owner");
            }
            // Validates the member and roles
            newCaseTeam.validate(newDefinition.getCaseTeamModel());
        }
    }

    @Override
    public String toString() {
        return "Migrate Case Definition '" + newDefinition.getName() + "'";
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        if (newDefinition.getDefinitionsDocument().equals(caseInstance.getDefinition().getDefinitionsDocument())) {
            // ??? why again???? ;)
            caseInstance.addDebugInfo(() -> "No need to migrate definition of case " + caseInstance.getId() + " (proposed definition already in use by the case instance)");
        } else {
            caseInstance.migrate(newDefinition, newCaseTeam);
        }
        setResponse(new MigrationStartedResponse(this));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.definition, newDefinition);
        writeField(generator, Fields.team, newCaseTeam);
    }
}
