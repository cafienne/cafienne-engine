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

package org.cafienne.engine.cmmn.test;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.cmmn.actorapi.command.CaseCommand;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * This is a helper class for the case unit test framework.
 * It performs actual logic inside the case and hence should not lead to state changes in the case.
 * Note, it can lead to recovery of the case.
 */
abstract class TestScriptCommand extends CaseCommand {
    private final String tenant;

    protected TestScriptCommand(String tenant, CaseUserIdentity user, String caseInstanceId) {
        super(user, caseInstanceId);
        this.tenant = tenant;
    }

    protected TestScriptCommand(ValueMap json) {
        super(json);
        this.tenant = json.readString(Fields.tenant);
    }

    abstract void beforeSendCommand(TestScript testScript);

    protected boolean isLocal() {
        return false;
    }

    @Override
    final public void validate(Case caseInstance) {
        // Avoid parents validate() logic, and just say it's fine.
    }

    @Override
    final public void processCaseCommand(Case caseInstance) {
        // No processing here required.
    }

    protected void waitSomeTime(long waitTime) {
        try {
            logger.debug("Sleeping " + waitTime + " milliseconds to 'execute' wait command ...");
            Thread.sleep(waitTime);
            logger.debug(" ... and continuing the test script");
        } catch (InterruptedException e) {
            logger.warn("The TestWaitCommand got interrrupted?!", e);
        }
    }

    protected void writeTestScriptCommand(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.tenant, tenant);
    }
}
