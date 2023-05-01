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

package org.cafienne.board.actorapi.response.runtime;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.board.actorapi.command.runtime.GetBoard;
import org.cafienne.board.actorapi.response.BoardResponseWithContent;
import org.cafienne.board.state.BoardState;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class GetBoardResponse extends BoardResponseWithContent {
    public final BoardState state;

    public GetBoardResponse(GetBoard command, BoardState state) {
        super(command);
        this.state = state;
    }

    public GetBoardResponse(ValueMap json) {
        super(json);
        this.state = BoardState.deserialize(json.with(Fields.state));
    }

    @Override
    public Value<?> toJson() {
        return new ValueMap(Fields.state, state);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.state, state);
    }
}
