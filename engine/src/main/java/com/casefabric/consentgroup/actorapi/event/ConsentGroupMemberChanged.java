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

package com.casefabric.consentgroup.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.consentgroup.ConsentGroupActor;
import com.casefabric.consentgroup.actorapi.ConsentGroupMember;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Manifest
public class ConsentGroupMemberChanged extends ConsentGroupMemberEvent {
    public final Set<String> rolesRemoved;

    public ConsentGroupMemberChanged(ConsentGroupActor group, ConsentGroupMember newMember, Set<String> rolesRemoved) {
        super(group, newMember);
        this.rolesRemoved = rolesRemoved;
    }

    public ConsentGroupMemberChanged(ValueMap json) {
        super(json);
        rolesRemoved = json.readSet(Fields.rolesRemoved);
    }

    @Override
    public void updateState(ConsentGroupActor group) {
        group.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeConsentGroupMemberEvent(generator);
        writeField(generator, Fields.rolesRemoved, rolesRemoved);
    }
}
