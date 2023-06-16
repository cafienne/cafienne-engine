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

package org.cafienne.board.actorapi.event.definition;

import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.event.BoardBaseEvent;
import org.cafienne.json.ValueMap;

public abstract class BoardDefinitionEvent extends BoardBaseEvent {

    public BoardDefinitionEvent(BoardActor board) {
        super(board);
    }

    public BoardDefinitionEvent(ValueMap json) {
        super(json);
    }
}
