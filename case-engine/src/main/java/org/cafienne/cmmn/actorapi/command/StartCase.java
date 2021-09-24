/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.event.CaseDefinitionApplied;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.actorapi.response.CaseStartedResponse;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.StringValue;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Manifest
public class StartCase extends CaseCommand implements BootstrapCommand {
    private final static Logger logger = LoggerFactory.getLogger(StartCase.class);

    private final String tenant;
    private final String rootCaseId;
    private final String parentCaseId;
    private final ValueMap inputParameters;
    private transient CaseDefinition definition;
    private CaseTeam caseTeamInput;
    private final boolean debugMode;

    /**
     * Starts a new case with the specified case definition
     *
     * @param inputs              The case input parameters
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseTeamInput       The CaseTeam for the case
     */
    public StartCase(TenantUser tenantUser, String caseInstanceId, CaseDefinition definition, ValueMap inputs, CaseTeam caseTeamInput) {
        this(tenantUser.tenant(), tenantUser, caseInstanceId, definition, inputs, caseTeamInput, Cafienne.config().actor().debugEnabled());
    }

    /**
     * Starts a new case with the specified case definition
     *
     * @param inputs              The case input parameters
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseTeamInput       The CaseTeam for the case
     * @param debugMode           Indication whether case should run in debug mode or not
     *
     */
    public StartCase(String tenant, TenantUser tenantUser, String caseInstanceId, CaseDefinition definition, ValueMap inputs, CaseTeam caseTeamInput, boolean debugMode) {
        this(tenant, tenantUser, caseInstanceId, definition, inputs, caseTeamInput, debugMode, "", caseInstanceId);
    }

    /**
     * Starts a new case with the specified case definition
     *
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseInputParameters The case input parameters
     * @param parentCaseId        The id of the parent case, if it exists
     * @param rootCaseId          The root case id, if it exists.
     */
    public StartCase(String tenant, TenantUser tenantUser, String caseInstanceId, CaseDefinition definition, ValueMap caseInputParameters, CaseTeam caseTeamInput, boolean debugMode, String parentCaseId, String rootCaseId) {
        super(tenantUser, caseInstanceId);
        // First validate the tenant information.
        this.tenant = tenant;
        if (tenant == null || tenant.isEmpty()) {
            throw new NullPointerException("Tenant cannot be null or empty");
        }
        this.definition = definition;
        this.rootCaseId = rootCaseId;
        this.parentCaseId = parentCaseId;
        this.inputParameters = caseInputParameters == null ? new ValueMap() : caseInputParameters;
        this.caseTeamInput = caseTeamInput == null ? CaseTeam.apply() : caseTeamInput;
        this.debugMode = debugMode;
    }

    public StartCase(ValueMap json) {
        super(json);
        this.tenant = readField(json, Fields.tenant);
        this.rootCaseId = readField(json, Fields.rootActorId);
        this.parentCaseId = readField(json, Fields.parentActorId);
        this.definition = readDefinition(json, Fields.definition, CaseDefinition.class);
        this.inputParameters = readMap(json, Fields.inputParameters);
        this.debugMode = readField(json, Fields.debugMode);
        this.caseTeamInput = CaseTeam.deserialize(json.withArray(Fields.team));
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
        if (caseTeamInput.getMembers().isEmpty()) {
            caseInstance.addDebugInfo(() -> "Adding user '" + getUser().id() + "' to the case team (as owner) because new team is empty");
            caseTeamInput = CaseTeam.apply(CaseTeamMember.createBootstrapMember(getUser()));
        }

        if (caseTeamInput.owners().isEmpty()) {
            throw new CaseTeamError("The case team needs to have at least one owner");
        }

        // Validates the member and roles
        caseTeamInput.validate(definition);

        // Should we also check whether all parameters have been made available in the input list? Not sure ...
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        caseInstance.upsertDebugMode(debugMode);
        if (debugMode) {
            caseInstance.addDebugInfo(() -> "Starting case "+ actorId +" of type "+ definition.getName()+" in debug mode");
        } else {
            caseInstance.addDebugInfo(() -> "Starting case "+ actorId +" of type "+ definition.getName());
        }

        // First set the definition
        caseInstance.addEvent(new CaseDefinitionApplied(caseInstance, rootCaseId, parentCaseId, definition));

        // First setup the case team, so that triggers or expressions in the case plan or case file can reason about the case team.
        caseInstance.getCaseTeam().fillFrom(caseTeamInput);

        // Apply input parameters. This may also fill the CaseFile
        caseInstance.addDebugInfo(() -> "Input parameters for new case of type "+ definition.getName(), inputParameters);
        caseInstance.setInputParameters(inputParameters);

        // Now trigger the Create transition on the case plan, to make the case actually go running
        caseInstance.createCasePlan();

        // Now also release the case file events that were generated while binding the case input parameters to the casefile
        caseInstance.releaseBootstrapCaseFileEvents();

        ValueMap json = new ValueMap();
        json.put("caseInstanceId", new StringValue(caseInstance.getId()));
        json.put("name", new StringValue(caseInstance.getDefinition().getName()));

        return new CaseStartedResponse(this, json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.tenant, tenant);
        writeListField(generator, Fields.team, caseTeamInput.getMembers());
        writeField(generator, Fields.inputParameters, inputParameters);
        writeField(generator, Fields.rootActorId, rootCaseId);
        writeField(generator, Fields.parentActorId, parentCaseId);
        writeField(generator, Fields.definition, definition);
        writeField(generator, Fields.debugMode, debugMode);
    }
}
