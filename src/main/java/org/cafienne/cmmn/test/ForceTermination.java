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
 * This is a helper class for the case unit test framework.
 * This removes the case model from memory
 */
@Manifest
public class ForceTermination extends TestScriptCommand {
    static {
        CafienneSerializer.addManifestWrapper(ForceTermination.class, ForceTermination::new);
    }

    public ForceTermination(String tenant, CaseUserIdentity user, String caseInstanceId) {
        super(tenant, user, caseInstanceId);
    }

    public ForceTermination(ValueMap json) {
        super(json);
    }

    @Override
    public void beforeSendCommand(TestScript testScript) {
        testScript.getCaseSystem().gateway().inform(new TerminateModelActor(getUser(), getActorId()), ActorRef.noSender());
    }

    @Override
    protected boolean isLocal() {
        return true;
    }

    @Override
    public String toString() {
        return "ForceTermination[" + getCaseInstanceId() + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
    }
}
