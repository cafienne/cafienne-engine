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

import org.cafienne.actormodel.identity.ConsentGroupUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroup;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ReplaceConsentGroup extends CollectiveCommand {
    public ReplaceConsentGroup(ConsentGroupUser user, ConsentGroup newGroupInfo) {
        super(user, newGroupInfo);
    }

    public ReplaceConsentGroup(ValueMap json) {
        super(json);
    }

    public boolean missingUserId(String userId) {
        return newGroupInfo.members().find(member -> member.userId().equals(userId)).isEmpty();
    }

    @Override
    public void processGroupCommand(ConsentGroupActor group) {
        group.replace(this);
    }
}
