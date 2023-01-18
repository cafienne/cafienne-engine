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

package org.cafienne.board.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.event.BoardBaseEvent;
import org.cafienne.board.actorapi.event.BoardEvent;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.actorapi.event.platform.PlatformBaseEvent;

import java.io.IOException;

@Manifest
public class BoardCreated extends BoardBaseEvent implements BootstrapMessage {
    public final String subject;
    public final CafienneVersion engineVersion;

    public BoardCreated(BoardActor board, String subject) {
        super(board);
        this.subject = subject;
        this.engineVersion = Cafienne.version();
    }

    public BoardCreated(ValueMap json) {
        super(json);
        this.subject = json.readString(Fields.subject);
        this.engineVersion = json.readObject(Fields.engineVersion, CafienneVersion::new);
    }

    @Override
    public void updateState(BoardActor board) {
        board.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeBoardEvent(generator);
        writeField(generator, Fields.subject, subject);
        writeField(generator, Fields.engineVersion, engineVersion.json());
    }
}
