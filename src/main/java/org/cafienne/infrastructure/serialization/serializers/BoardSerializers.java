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

package org.cafienne.infrastructure.serialization.serializers;

import org.cafienne.board.actorapi.event.BoardCreated;
import org.cafienne.board.actorapi.event.BoardModified;
import org.cafienne.infrastructure.serialization.CafienneSerializer;

public class BoardSerializers {
    public static void register() {
        addBoardCommands();
        addBoardEvents();
    }

    private static void addBoardCommands() {
    }

    private static void addBoardEvents() {
        CafienneSerializer.addManifestWrapper(BoardCreated.class, BoardCreated::new);
        CafienneSerializer.addManifestWrapper(BoardModified.class, BoardModified::new);
    }
}
