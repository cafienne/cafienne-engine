/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.debug;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.event.DebugDisabled;
import org.cafienne.cmmn.akka.event.DebugEnabled;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;

import java.io.IOException;

/**
 * Mechanism to switch a case instance from/to debug logging as separate events.
 *
 */
@Manifest
public class SwitchDebugMode extends CaseCommand {
    private final boolean debugMode;

    private enum Fields {
        debugMode
    }

    /**
     * Starts a new case with the specified name from the definitions document
     *
     * @param caseInstanceId      The instance identifier of the case
     * @param debugMode           True if debug must be enabled, false if disabled.
     */
    public SwitchDebugMode(TenantUser tenantUser, String caseInstanceId, boolean debugMode) {
        super(tenantUser, caseInstanceId);
        this.debugMode = debugMode;
    }

    public SwitchDebugMode(ValueMap json) {
        super(json);
        this.debugMode = readField(json, Fields.debugMode);
    }

    @Override
    public String toString() {
        return "Setting debug mode of case '" + getCaseInstanceId() + "' to "+debugMode;
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        if (debugMode!=caseInstance.debugMode()) {
            if (debugMode) caseInstance.addEvent(new DebugEnabled(caseInstance));
            else caseInstance.addEvent(new DebugDisabled(caseInstance));
        }
        return new CaseResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.debugMode, debugMode);
    }
}
