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

package com.casefabric.cmmn.actorapi.command.debug;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * Mechanism to switch a case instance from/to debug logging as separate events.
 *
 */
@Manifest
public class SwitchDebugMode extends CaseCommand {
    private final boolean debugMode;

    /**
     * Starts a new case with the specified name from the definitions document
     *
     * @param caseInstanceId      The instance identifier of the case
     * @param debugMode           True if debug must be enabled, false if disabled.
     */
    public SwitchDebugMode(CaseUserIdentity user, String caseInstanceId, boolean debugMode) {
        super(user, caseInstanceId);
        this.debugMode = debugMode;
    }

    public SwitchDebugMode(ValueMap json) {
        super(json);
        this.debugMode = json.readBoolean(Fields.debugMode);
    }

    @Override
    public String toString() {
        return "Setting debug mode of case '" + getCaseInstanceId() + "' to "+debugMode;
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        caseInstance.upsertDebugMode(debugMode);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.debugMode, debugMode);
    }
}
