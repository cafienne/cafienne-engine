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

package org.cafienne.board.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.ActorModified;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.board.BoardActor;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;

import java.io.IOException;

/**
 * Event that is published after an {@link org.cafienne.tenant.actorapi.command.TenantCommand} has been fully handled by a {@link TenantActor} instance.
 * Contains information about the last modified moment.
 *
 */
@Manifest
public class BoardModified extends ActorModified<BoardActor> implements BoardEvent {
    protected final CaseDefinition definition;

    public BoardModified(BoardActor actor, IncomingActorMessage source) {
        super(actor, source);
        this.definition = actor.getDefinition().caseDefinition();
    }

    public BoardModified(ValueMap json) {
        super(json);
        this.definition = json.readDefinition(Fields.definition, CaseDefinition.class);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.definition, definition);
    }
}
