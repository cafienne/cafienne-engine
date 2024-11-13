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

package org.cafienne.consentgroup.actorapi.response;

import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

@Manifest
public class ConsentGroupCreatedResponse extends ConsentGroupResponse {
    public ConsentGroupCreatedResponse(ConsentGroupCommand command) {
        super(command);
    }

    public ConsentGroupCreatedResponse(ValueMap json) {
        super(json);
    }

    @Override
    public Value<?> toJson() {
        return new ValueMap(Fields.groupId, getActorId());
    }
}
