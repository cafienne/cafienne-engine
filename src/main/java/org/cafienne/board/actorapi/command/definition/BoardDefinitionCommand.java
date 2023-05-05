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

package org.cafienne.board.actorapi.command.definition;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.actormodel.response.ActorLastModified;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.command.BoardCommand;
import org.cafienne.board.actorapi.response.BoardResponse;
import org.cafienne.board.state.definition.BoardDefinition;

/**
 */
public abstract class BoardDefinitionCommand extends BoardCommand {
    /**
     * Create a new command that can be sent to the board.
     *
     * @param user The user that issues this command.
     */
    protected BoardDefinitionCommand(BoardUser user) {
        super(user);
    }

    @Override
    protected boolean isAsync() {
        return true;
    }

    public void validate(BoardActor board) throws InvalidCommandException {
        super.validate(board);
        if (! board.state.boardManagers().contains(getUser().id())) {
            throw new AuthorizationException("Only bord managers can change board definitions");
        }
        validate(board.state.definition());
    }

    public void validate(BoardDefinition definition) throws InvalidCommandException {
    }

    @Override
    public void processBoardCommand(BoardActor board) {
        processBoardDefinitionCommand(board.state.definition());
    }

    protected abstract void processBoardDefinitionCommand(BoardDefinition definition);

    public BoardResponse createResponse(ActorLastModified lastModified) {
        return new BoardResponse(this, lastModified);
    }
}
