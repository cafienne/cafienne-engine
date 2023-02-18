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

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.event.BoardCreated;
import org.cafienne.board.actorapi.response.BoardCreatedResponse;
import org.cafienne.board.actorapi.response.BoardResponse;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class CreateBoard extends BoardCommand implements BootstrapMessage {
    public final String title;

    public CreateBoard(BoardUser user, String title) {
        super(user);
        this.title = title;
    }

    public CreateBoard(ValueMap json) {
        super(json);
        this.title = json.readString(Fields.title);
    }

    @Override
    public String tenant() {
        return Cafienne.config().platform().defaultTenant();
    }

    @Override
    public void validate(BoardActor tenant) throws InvalidCommandException {
        super.validate(tenant);
    }

    @Override
    public BoardResponse process(BoardActor board) {
        board.addEvent(new BoardCreated(board, title));
        return new BoardCreatedResponse(this, board.getId());
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.title, title);
    }
}

