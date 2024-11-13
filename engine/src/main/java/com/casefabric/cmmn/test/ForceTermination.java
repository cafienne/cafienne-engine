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

package com.casefabric.cmmn.test;

import org.apache.pekko.actor.ActorRef;
import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.command.TerminateModelActor;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.infrastructure.serialization.CaseFabricSerializer;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

/**
 * This is a helper class for the case unit test framework.
 * This removes the case model from memory
 */
@Manifest
public class ForceTermination extends TestScriptCommand {
    static {
        CaseFabricSerializer.addManifestWrapper(ForceTermination.class, ForceTermination::new);
    }

    public ForceTermination(String tenant, CaseUserIdentity user, String caseInstanceId) {
        super(tenant, user, caseInstanceId);
    }

    public ForceTermination(ValueMap json) {
        super(json);
    }

    @Override
    public void beforeSendCommand(TestScript testScript) {
        testScript.getCaseSystem().gateway().inform(new TerminateModelActor(getActorId()), ActorRef.noSender());
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
