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

package com.casefabric.consentgroup.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.ConsentGroupUser;
import com.casefabric.consentgroup.ConsentGroupActor;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class RemoveConsentGroupMember extends ConsentGroupCommand {
    public final String userId;

    public RemoveConsentGroupMember(ConsentGroupUser groupOwner, String userId) {
        super(groupOwner, groupOwner.groupId());
        this.userId = userId;
    }

    public RemoveConsentGroupMember(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void validate(ConsentGroupActor group) throws InvalidCommandException {
        super.validate(group);
        validateNotLastMember(group, userId);
        validateNotLastOwner(group, userId);
    }

    @Override
    public void processGroupCommand(ConsentGroupActor group) {
        group.removeMember(userId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}