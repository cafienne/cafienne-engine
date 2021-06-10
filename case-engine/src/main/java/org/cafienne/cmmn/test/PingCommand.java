/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.CafienneSerializable;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.akka.actor.serialization.json.LongValue;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This is a helper class for the case unit test framework. Within a case, one can use timer events. When running a case unit test, sometimes after triggering a command a timer will run. The test may
 * need to wait a certain time before continuing, in order to wait for the "after-timer" actions.
 */
@Manifest
public class PingCommand extends CaseCommand implements CafienneSerializable, BootstrapCommand {
    private final static Logger logger = LoggerFactory.getLogger(PingCommand.class);

    private final long waitTime;

    private final String tenant;

    public PingCommand(TenantUser tenantUser, String caseInstanceId, long waitTimeInMillis) {
        super(tenantUser, caseInstanceId);
        this.tenant = tenantUser.tenant();
        this.waitTime = waitTimeInMillis;
    }

    public PingCommand(ValueMap json) {
        super(json);
        this.waitTime = Long.parseLong(json.raw(Fields.waitTime));
        this.tenant = readField(json, Fields.tenant);
    }

    @Override
    public String tenant() {
        return tenant;
    }

    @Override
    public void validate(Case caseInstance) {
        // Avoid parents validate() logic, and just say it's fine.
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        // No processing here required.
        return new CaseResponse(this);
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
    void awaitCompletion() {
        try {
            logger.debug("Sleeping " + waitTime + " milliseconds to 'execute' wait command ...");
            Thread.sleep(waitTime);
            logger.debug(" ... and continuing the test script");
        } catch (InterruptedException e) {
            logger.warn("The TestWaitCommand got interrrupted?!", e);
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.waitTime, new LongValue(waitTime));
        writeField(generator, Fields.tenant, tenant);
    }
}
