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
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.LongValue;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This is a helper class for the case unit test framework. Within a case, one can use timer events. When running a case unit test, sometimes after triggering a command a timer will run. The test may
 * need to wait a certain time before continuing, in order to wait for the "after-timer" actions.
 */
@Manifest
public class PingCommand extends TestScriptCommand {
    private final static Logger logger = LoggerFactory.getLogger(PingCommand.class);

    private final long waitTime;

    public PingCommand(String tenant, CaseUserIdentity user, String caseInstanceId, long waitTimeInMillis) {
        super(tenant, user, caseInstanceId);
        this.waitTime = waitTimeInMillis;
    }

    public PingCommand(ValueMap json) {
        super(json);
        this.waitTime = Long.parseLong(json.raw(Fields.waitTime));
    }

    @Override
    public String toString() {
        return "Ping "+waitTime+"ms";
    }

    /**
     * This method is known to the CaseTestingActor and holds the actual logic of the command.
     * The point here is, that the CaseTestingActor must wait a period in order to have
     * the akka framework to proceed e.g. timer events. If this waiting is done inside the {@link PingCommand#process(Case)} method,
     * the waiting is done when the case actor processes the command - which then blocks the akka framework.
     */
    @Override
    public void beforeSendCommand(TestScript testScript) {
        waitSomeTime(waitTime);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeTestScriptCommand(generator);
        writeField(generator, Fields.waitTime, new LongValue(waitTime));
    }
}
