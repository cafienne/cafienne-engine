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

package org.cafienne.board.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.board.BoardActor;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class BoardManagerRemoved extends BoardTeamEvent {

    public final String userId;

    public BoardManagerRemoved(BoardActor board, String userId) {
        super(board);
        this.userId = userId;
    }

    public BoardManagerRemoved(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeBoardTeamEvent(generator);
        writeField(generator, Fields.userId, userId);
    }
}
