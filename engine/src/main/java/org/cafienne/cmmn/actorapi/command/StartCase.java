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

package org.cafienne.cmmn.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.actorapi.event.CaseDefinitionApplied;
import org.cafienne.cmmn.actorapi.response.CaseStartedResponse;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.StringValue;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class StartCase extends CaseCommand implements BootstrapMessage {
    protected final String tenant;
    protected final String rootCaseId;
    protected final String parentCaseId;
    protected final ValueMap inputParameters;
    protected final CaseDefinition definition;
    protected CaseTeam caseTeam;
    protected final boolean debugMode;

    /**
     * Starts a new case with the specified case definition
     *
     * @param inputs              The case input parameters
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseTeam            The CaseTeam for the case
     * @param debugMode           Indication whether case should run in debug mode or not
     *
     */
    public StartCase(String tenant, CaseUserIdentity user, String caseInstanceId, CaseDefinition definition, ValueMap inputs, CaseTeam caseTeam, boolean debugMode) {
        this(tenant, user, caseInstanceId, definition, inputs, caseTeam, debugMode, "", caseInstanceId);
    }

    /**
     * Starts a new case with the specified case definition
     *
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseInputParameters The case input parameters
     * @param caseTeam            The CaseTeam for the case
     * @param debugMode           Indication whether case should run in debug mode or not
     * @param parentCaseId        The id of the parent case, if it exists
     * @param rootCaseId          The root case id, if it exists.
     */
    public StartCase(String tenant, CaseUserIdentity user, String caseInstanceId, CaseDefinition definition, ValueMap caseInputParameters, CaseTeam caseTeam, boolean debugMode, String parentCaseId, String rootCaseId) {
        super(user, caseInstanceId);
        // First validate the tenant information.
        this.tenant = tenant;
        if (tenant == null || tenant.isEmpty()) {
            throw new NullPointerException("Tenant cannot be null or empty");
        }
        this.definition = definition;
        this.rootCaseId = rootCaseId;
        this.parentCaseId = parentCaseId;
        this.inputParameters = caseInputParameters == null ? new ValueMap() : caseInputParameters;
        this.caseTeam = caseTeam;
        this.debugMode = debugMode;
    }

    public StartCase(ValueMap json) {
        super(json);
        this.tenant = json.readString(Fields.tenant);
        this.rootCaseId = json.readString(Fields.rootActorId);
        this.parentCaseId = json.readString(Fields.parentActorId);
        this.definition = json.readDefinition(Fields.definition, CaseDefinition.class);
        this.inputParameters = json.readMap(Fields.inputParameters);
        this.debugMode = json.readBoolean(Fields.debugMode);
        this.caseTeam = json.readObject(Fields.team, CaseTeam::deserialize);
    }

    @Override
    public String tenant() {
        return tenant;
    }

    @Override
    public String toString() {
        return "Set Case Definition '" + definition.getName() + "'";
    }

    @Override
    public void validate(Case caseInstance) {
        if (caseInstance.getDefinition() != null) {
            throw new InvalidCommandException("Cannot apply a new case definition; the case already has a definition");
        }

        // Now validate the input parameters - especially whether they exist in the definition
        inputParameters.getValue().forEach((inputParameterName, value) -> {
            InputParameterDefinition inputParameterDefinition = definition.getInputParameters().get(inputParameterName);
            if (inputParameterDefinition == null) { // Validate whether this input parameter actually exists in the Case
                throw new InvalidCommandException("An input parameter with name " + inputParameterName + " is not defined in the case");
            }
            inputParameterDefinition.validate(value);
        });

        // If the case team is empty, add current user both as member and as owner,
        //  including default mapping of tenant roles to case team roles
        if (caseTeam.isEmpty()) {
            caseInstance.addDebugInfo(() -> "Adding user '" + getUser().id() + "' to the case team (as owner) because new team is empty");
            caseTeam = CaseTeam.create(getUser());
        }

        if (caseTeam.owners().isEmpty()) {
            throw new CaseTeamError("The case team needs to have at least one owner");
        }

        // Validates the member and roles
        caseTeam.validate(definition.getCaseTeamModel());

        // Should we also check whether all parameters have been made available in the input list? Not sure ...
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        caseInstance.upsertDebugMode(debugMode);
        if (debugMode) {
            caseInstance.addDebugInfo(() -> "Starting case "+ actorId +" of type "+ definition.getName()+" in debug mode");
        } else {
            caseInstance.addDebugInfo(() -> "Starting case "+ actorId +" of type "+ definition.getName());
        }

        // First set the definition
        caseInstance.addEvent(new CaseDefinitionApplied(caseInstance, rootCaseId, parentCaseId, definition));

        // First setup the case team, so that triggers or expressions in the case plan or case file can reason about the case team.
        caseInstance.getCaseTeam().create(caseTeam);

        // Apply input parameters. This may also fill the CaseFile
        caseInstance.addDebugInfo(() -> "Input parameters for new case of type " + definition.getName(), inputParameters);
        caseInstance.setInputParameters(inputParameters);

        // Now trigger the Create transition on the case plan, to make the case actually go running
        caseInstance.createCasePlan();

        // Now also release the case file events that were generated while binding the case input parameters to the casefile
        caseInstance.releaseBootstrapCaseFileEvents();

        ValueMap json = new ValueMap();
        json.put("caseInstanceId", new StringValue(caseInstance.getId()));
        json.put("name", new StringValue(caseInstance.getDefinition().getName()));

        setResponse(new CaseStartedResponse(this, json));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.tenant, tenant);
        writeField(generator, Fields.team, caseTeam);
        writeField(generator, Fields.inputParameters, inputParameters);
        writeField(generator, Fields.rootActorId, rootCaseId);
        writeField(generator, Fields.parentActorId, parentCaseId);
        writeField(generator, Fields.definition, definition);
        writeField(generator, Fields.debugMode, debugMode);
    }
}
