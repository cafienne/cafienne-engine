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

package org.cafienne.actormodel.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class TerminateModelActor extends BaseModelCommand<ModelActor, UserIdentity> {
    static {
        // Only register this one when used ;)
        CafienneSerializer.addManifestWrapper(TerminateModelActor.class, TerminateModelActor::new);
    }

    public final String actorId;
    public final boolean needsResponse;

    public TerminateModelActor(UserIdentity user, String actorId) {
        this(user, actorId, true);
    }

    public TerminateModelActor(UserIdentity user, String actorId, boolean needsResponse) {
        super(user, actorId);
        this.actorId = actorId;
        this.needsResponse = needsResponse;
    }

    public TerminateModelActor(ValueMap json) {
        super(json);
        this.actorId = json.readString(Fields.actorId);
        this.needsResponse = json.readBoolean((Fields.response), true);
    }

    @Override
    public UserIdentity readUser(ValueMap json) {
        return null;
    }

    @Override
    public void validate(ModelActor modelActor) throws InvalidCommandException {
    }

    @Override
    public ModelResponse process(ModelActor modelActor) {
        return null;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeField(generator, Fields.actorId, actorId);
        writeField(generator, Fields.response, needsResponse);
    }
}

