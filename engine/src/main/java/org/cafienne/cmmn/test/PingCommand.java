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
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.LongValue;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * This is a helper class for the case unit test framework. Within a case, one can use timer events. When running a case unit test, sometimes after triggering a command a timer will run. The test may
 * need to wait a certain time before continuing, in order to wait for the "after-timer" actions.
 */
@Manifest
public class PingCommand extends TestScriptCommand {
    static {
        CafienneSerializer.addManifestWrapper(PingCommand.class, PingCommand::new);
    }

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
     * the actor framework to proceed e.g. timer events. If this waiting is done inside the {@link PingCommand#process(Case)} method,
     * the waiting is done when the case actor processes the command - which then blocks the actor framework.
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
