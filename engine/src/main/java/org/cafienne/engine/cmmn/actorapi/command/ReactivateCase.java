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

package org.cafienne.engine.cmmn.actorapi.command;

import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.engine.cmmn.actorapi.event.CaseDefinitionApplied;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.Transition;
import org.cafienne.engine.cmmn.instance.team.CaseTeamError;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ReactivateCase extends StartCase {
    @Override
    public boolean isBootstrapMessage() {
        return false;
    }

    /**
     * Reactivate an existing sub case with the specified case definition
     *
     * @param caseInstanceId      The instance identifier for the new case
     * @param definition          The case definition (according to the CMMN xsd) that is being used to execute the model
     * @param caseInputParameters The case input parameters
     * @param caseTeam            The CaseTeam for the case
     * @param debugMode           Indication whether case should run in debug mode or not
     * @param parentCaseId        The id of the parent case, if it exists
     * @param rootCaseId          The root case id, if it exists.
     */
    public ReactivateCase(String tenant, CaseUserIdentity user, String caseInstanceId, CaseDefinition definition, ValueMap caseInputParameters, CaseTeam caseTeam, boolean debugMode, String parentCaseId, String rootCaseId) {
        super(tenant, user, caseInstanceId, definition, caseInputParameters, caseTeam, debugMode, parentCaseId, rootCaseId);
    }

    public ReactivateCase(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "Reactivate Case '" + definition.getName() + "'";
    }

    @Override
    public void validate(Case caseInstance) {
        if (caseInstance.getCasePlan() == null) {
            // Case was never instantiated
            super.validate(caseInstance);
        } else {

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
        }
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        if (caseInstance.getCasePlan() == null) {
            // Case was never instantiated
            super.processCaseCommand(caseInstance);
        } else {
            // First replace the case team, so that triggers or expressions in the case plan or case file can reason about the case team.
            caseInstance.getCaseTeam().replace(caseTeam);

            // Apply input parameters. This may also fill the CaseFile
            caseInstance.addDebugInfo(() -> "Input parameters for new case of type " + definition.getName(), inputParameters);
            caseInstance.setInputParameters(inputParameters);

            caseInstance.getCasePlan().makeTransition(Transition.Reactivate);
        }
    }
}
