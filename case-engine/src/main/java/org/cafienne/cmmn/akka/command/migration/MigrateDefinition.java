/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.StringValue;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.akka.command.response.CaseStartedResponse;
import org.cafienne.cmmn.akka.command.response.migration.MigrationStartedResponse;
import org.cafienne.cmmn.akka.command.team.CaseTeam;
import org.cafienne.cmmn.akka.command.team.CaseTeamMember;
import org.cafienne.cmmn.akka.event.CaseDefinitionApplied;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Manifest
public class MigrateDefinition extends CaseCommand {
    private transient CaseDefinition definition;
    private final String migrationScript;

    /**
     * Migrate the definition of a case.
     *
     * @param caseInstanceId      The instance identifier of the case
     * @param definition          The case definition (according to the CMMN xsd) to be updated to
     */
    public MigrateDefinition(TenantUser tenantUser, String caseInstanceId, CaseDefinition definition, String migrationScript) {
        super(tenantUser, caseInstanceId);
        this.definition = definition;
        this.migrationScript = migrationScript;
    }

    public MigrateDefinition(ValueMap json) {
        super(json);
        this.definition = readDefinition(json, Fields.definition, CaseDefinition.class);
        this.migrationScript = readField(json, Fields.source);
    }

    @Override
    public String toString() {
        return "Migrate Case Definition '" + definition.getName() + "'";
    }

    @Override
    public void validate(Case caseInstance) {
        // Should we also check whether all parameters have been made available in the input list? Not sure ...
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        caseInstance.migrate(definition, migrationScript);
        ValueMap json = new ValueMap("caseInstance", getCaseInstanceId(), "state", caseInstance.getStateAsValueMap());
        return new MigrationStartedResponse(this, json);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.definition, definition);
        writeField(generator, Fields.source, migrationScript);
    }
}
