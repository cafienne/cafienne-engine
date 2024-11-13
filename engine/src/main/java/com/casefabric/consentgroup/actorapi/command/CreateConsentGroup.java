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

import com.casefabric.actormodel.command.BootstrapMessage;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.ConsentGroupUser;
import com.casefabric.consentgroup.ConsentGroupActor;
import com.casefabric.consentgroup.actorapi.ConsentGroup;
import com.casefabric.consentgroup.actorapi.exception.ConsentGroupException;
import com.casefabric.consentgroup.actorapi.response.ConsentGroupCreatedResponse;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

@Manifest
public class CreateConsentGroup extends CollectiveCommand implements BootstrapMessage {
    public CreateConsentGroup(ConsentGroupUser user, ConsentGroup newGroupInfo) {
        super(user, newGroupInfo);
    }

    public CreateConsentGroup(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(ConsentGroupActor groupActor) throws InvalidCommandException {
        if (groupActor.exists()) {
            throw new ConsentGroupException("Consent group already exists");
        }
    }

    @Override
    public void processGroupCommand(ConsentGroupActor group) {
        group.create(this);
        setResponse(new ConsentGroupCreatedResponse(this));
    }

    @Override
    public String tenant() {
        return newGroupInfo.tenant();
    }
}
