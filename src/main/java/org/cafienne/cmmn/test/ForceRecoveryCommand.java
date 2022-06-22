/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import akka.actor.ActorRef;
import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.TerminateModelActor;
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
        testScript.getCaseSystem().gateway().inform(new TerminateModelActor(getUser(), getActorId()), ActorRef.noSender());
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
