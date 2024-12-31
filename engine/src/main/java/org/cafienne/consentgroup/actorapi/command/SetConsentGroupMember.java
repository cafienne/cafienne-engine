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

package org.cafienne.consentgroup.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.ConsentGroupUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroupMember;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class SetConsentGroupMember extends ConsentGroupCommand {
    private final ConsentGroupMember newMemberInfo;

    public SetConsentGroupMember(ConsentGroupUser groupOwner, ConsentGroupMember newMemberInfo) {
        super(groupOwner, groupOwner.groupId());
        this.newMemberInfo = newMemberInfo;
    }

    public SetConsentGroupMember(ValueMap json) {
        super(json);
        this.newMemberInfo = ConsentGroupMember.deserialize(json.with(Fields.member));
    }

    @Override
    public void validate(ConsentGroupActor group) throws InvalidCommandException {
        super.validate(group);
        // Check that new member is not last owner
        if (! newMemberInfo.isOwner()) {
            // ... then check this member is not the last owner.
            validateNotLastOwner(group, newMemberInfo.userId());
        }
    }

    @Override
    public void processGroupCommand(ConsentGroupActor group) {
        group.setMember(newMemberInfo);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.member, newMemberInfo.toValue());
    }
}