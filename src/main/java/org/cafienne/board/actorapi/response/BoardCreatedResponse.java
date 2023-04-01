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

package org.cafienne.board.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.response.ActorLastModified;
import org.cafienne.board.actorapi.command.CreateBoard;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class BoardCreatedResponse extends BoardResponseWithContent {
    public final String boardId;

    public BoardCreatedResponse(CreateBoard command, String boardId, ActorLastModified consentGroupCreated) {
        super(command);
        // We override last modified with the one from the consent group, in order to have the getBoardUser info work properly
        setLastModified(consentGroupCreated);
        this.boardId = boardId;
    }

    public BoardCreatedResponse(ValueMap json) {
        super(json);
        this.boardId = json.readString(Fields.boardId);
    }

    @Override
    public Value<?> toJson() {
        return new ValueMap(Fields.boardId, boardId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.boardId, boardId);
    }
}
