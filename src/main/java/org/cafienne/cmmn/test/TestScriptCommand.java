/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
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
    final public CaseResponse process(Case caseInstance) {
        // No processing here required.
        return new CaseResponse(this);
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
