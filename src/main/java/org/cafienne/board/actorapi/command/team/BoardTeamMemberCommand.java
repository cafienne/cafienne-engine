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

package org.cafienne.board.actorapi.command.team;

import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.command.BoardCommand;

/**
 * Base class for updating board teams
 */
public abstract class BoardTeamMemberCommand extends BoardCommand {
    /**
     * Create a new command that can be sent to the board.
     *
     * @param user The user that issues this command.
     */
    protected BoardTeamMemberCommand(BoardUser user) {
        super(user);
    }

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
    public void processBoardCommand(BoardActor board) {
        board.state.team().handle(this);
    }
}
