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

package org.cafienne.cmmn.test;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * This is a helper class for the case unit test framework. Within a case, one can use timer events. When running a case unit test, sometimes after triggering a command a timer will run. The test may
 * need to wait a certain time before continuing, in order to wait for the "after-timer" actions.
 */
@Manifest
public class ForceRecoveryCommand extends TestScriptCommand {
    static {
        CafienneSerializer.addManifestWrapper(ForceRecoveryCommand.class, ForceRecoveryCommand::new);
    }

    public ForceRecoveryCommand(String tenant, CaseUserIdentity user, String caseInstanceId) {
        super(tenant, user, caseInstanceId);
    }

    public ForceRecoveryCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void beforeSendCommand(TestScript testScript) {
        testScript.getCaseSystem().engine().terminate(getActorId());
        // Give the system 500 ms to clean up the actor and the references
        waitSomeTime(500);
    }

    @Override
    public String toString() {
        return "ForceRecovery[" + getCaseInstanceId() + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}
