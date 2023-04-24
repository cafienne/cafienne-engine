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

package org.cafienne.board.actorapi.command.runtime;

import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.command.BoardCommand;
import org.cafienne.board.actorapi.response.runtime.GetBoardResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class GetBoard extends BoardCommand {

    public GetBoard(BoardUser user) {
        super(user);
    }

    public GetBoard(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(BoardActor board) throws InvalidCommandException {
        super.validate(board);
    }

    @Override
    public void processBoardCommand(BoardActor board) {
        setResponse(new GetBoardResponse(this, board.state));
    }
}

