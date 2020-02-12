/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.akka.command.response.CaseStartedResponse;
import org.cafienne.cmmn.akka.command.team.CaseTeam;
import org.cafienne.cmmn.akka.event.CaseDefinitionApplied;
import org.cafienne.cmmn.akka.event.debug.DebugEnabled;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.StringValue;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

@Manifest
public class StartCase extends CaseCommand implements BootstrapCommand {
    private final static Logger logger = LoggerFactory.getLogger(StartCase.class);

    private final String tenant;
    private final String rootCaseId;
    private final String parentCaseId;
    private final ValueMap inputParameters;
    private transient CaseDefinition definition;
    private final CaseTeam caseTeamInput;
    private org.cafienne.cmmn.user.CaseTeam caseTeam;
    private final boolean debugMode;

    private enum Fields {
        tenant, name, parentActorId, rootActorId, caseTeam, inputParameters, definition, debugMode
    }

    /**
     * Starts a new case with the specified case definition
     *
     * @param inputs              The case input parameters
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseTeamInput       The CaseTeam for the case
     */
    public StartCase(TenantUser tenantUser, String caseInstanceId, CaseDefinition definition, ValueMap inputs, CaseTeam caseTeamInput) {
        this(tenantUser.tenant(), tenantUser, caseInstanceId, definition, inputs, caseTeamInput, CaseSystem.debugEnabled());
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
            throw new IllegalArgumentException("Tenant cannot be null or empty");
        }
        this.definition = definition;
        this.rootCaseId = rootCaseId;
        this.parentCaseId = parentCaseId;
        this.inputParameters = caseInputParameters == null ? new ValueMap() : caseInputParameters;
        this.caseTeamInput = caseTeamInput == null ? new CaseTeam() : caseTeamInput;
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
        this.caseTeamInput = new CaseTeam(readArray(json, Fields.caseTeam));
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
        Iterator<String> keys = inputParameters.fieldNames();
        while (keys.hasNext()) {
            String inputParameterName = keys.next();
            InputParameterDefinition def = definition.getInputParameters().get(inputParameterName);
            if (def == null) { // Validate whether this input parameter actually exists in the Case
                throw new InvalidCommandException("An input parameter with name " + inputParameterName + " is not defined in the case");
            }
        }

        // Validates the member and roles
        caseTeam = new org.cafienne.cmmn.user.CaseTeam(caseTeamInput, caseInstance, definition);

        // Should we also check whether all parameters have been made available in the input list? Not sure ...
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        if (debugMode) {
            caseInstance.addEvent(new DebugEnabled(caseInstance));
            logger.debug("Starting case "+ actorId +" of type "+ definition.getName()+" in debug mode");
        } else {
            logger.debug("Starting case "+ actorId +" of type "+ definition.getName());
        }

        // First set the definition
        caseInstance.addEvent(new CaseDefinitionApplied(caseInstance, rootCaseId, parentCaseId, definition, definition.getName())).finished();

        // Apply input parameters. This may also fill the CaseFile
        caseInstance.addDebugInfo(DebugEvent.class, e -> e.addMessage("Input parameters for new case of type "+ definition.getName(), inputParameters));
        caseInstance.setInputParameters(inputParameters);

        // Add members to case team
        caseTeam.getMembers().forEach(newMember -> caseInstance.getCaseTeam().addMember(newMember));

        // Now trigger the Create transition on the case plan, to make the case actually go running
        PlanItem casePlan = caseInstance.getCasePlan();
        caseInstance.makePlanItemTransition(casePlan, Transition.Create);

        ValueMap json = new ValueMap();
        json.put("caseInstanceId", new StringValue(caseInstance.getId()));
        json.put("name", new StringValue(caseInstance.getDefinition().getName()));

        return new CaseStartedResponse(this, json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.tenant, tenant);
        writeField(generator, Fields.caseTeam, caseTeamInput);
        writeField(generator, Fields.inputParameters, inputParameters);
        writeField(generator, Fields.rootActorId, rootCaseId);
        writeField(generator, Fields.parentActorId, parentCaseId);
        writeField(generator, Fields.definition, definition);
        writeField(generator, Fields.debugMode, debugMode);
    }
}
