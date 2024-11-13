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

package com.casefabric.cmmn.actorapi.command.migration;

import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.team.CaseTeam;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

@Manifest
public class MigrateDefinition extends MigrateCaseDefinition {
    /**
     * Migrate the definition of a case.
     *
     * @param caseInstanceId The instance identifier of the case
     * @param newDefinition  The case definition (according to the CMMN xsd) to be updated to
     */
    public MigrateDefinition(CaseUserIdentity user, String caseInstanceId, CaseDefinition newDefinition, CaseTeam newTeam) {
        super(user, caseInstanceId, newDefinition, newTeam);
    }

    public MigrateDefinition(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        if (! caseInstance.getParentActorId().isEmpty()) {
            throw new InvalidCommandException("Cannot update the definition of a sub case directly. This must be done through the parent case");
        }
    }
}
