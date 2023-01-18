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

package org.cafienne.board.actorapi.command;

import org.cafienne.actormodel.command.BaseModelCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.BoardMessage;
import org.cafienne.json.ValueMap;

/**
 * Base class for sending commands to a TenantActor
 */
public abstract class BoardCommand extends BaseModelCommand<BoardActor, BoardUser> implements BoardMessage {
    /**
     * Create a new command that can be sent to the board.
     *
     * @param user The user that issues this command.
     * @param boardId    Id of the board to send the command to
     */
    protected BoardCommand(BoardUser user, String boardId) {
        super(user, boardId);
    }

    protected BoardCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected BoardUser readUser(ValueMap json) {
        return BoardUser.deserialize(json);
    }

    /**
     * Hook to validate the command.
     *
     * @param board
     * @throws InvalidCommandException If the command is invalid
     */
    public void validate(BoardActor board) throws InvalidCommandException {
    }
}
