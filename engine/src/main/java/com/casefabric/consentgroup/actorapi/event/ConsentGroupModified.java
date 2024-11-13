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

import com.casefabric.actormodel.event.ActorModified;
import com.casefabric.actormodel.message.IncomingActorMessage;
import com.casefabric.consentgroup.ConsentGroupActor;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Event that is published after an {@link com.casefabric.consentgroup.actorapi.command.ConsentGroupCommand} has been fully handled by a {@link ConsentGroupActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class ConsentGroupModified extends ActorModified<ConsentGroupActor> implements ConsentGroupEvent  {
    public ConsentGroupModified(ConsentGroupActor actor, IncomingActorMessage source) {
        super(actor, source);
    }

    public ConsentGroupModified(ValueMap json) {
        super(json);
    }
}
