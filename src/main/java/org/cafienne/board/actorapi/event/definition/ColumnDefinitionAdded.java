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

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.board.BoardActor;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ColumnDefinitionAdded extends BoardDefinitionEvent {
    public final String columnId;
    public final String title;
    public final scala.Option<String> role;
    public final scala.Option<ValueMap> form;

    public ColumnDefinitionAdded(BoardActor board, String columnId, String title, scala.Option<String> role, scala.Option<ValueMap> form) {
        super(board);
        this.columnId = columnId;
        this.title = title;
        this.role = role;
        this.form = form;
    }

    public ColumnDefinitionAdded(ValueMap json) {
        super(json);
        this.columnId = json.readString(Fields.columnId);
        this.title = json.readString(Fields.title);
        this.role = json.readOption(Fields.role);
        this.form = json.readOptionalMap(Fields.form);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeBoardEvent(generator);
        writeField(generator, Fields.columnId, columnId);
        writeField(generator, Fields.title, title);
        writeField(generator, Fields.role, role);
        writeField(generator, Fields.form, form);
    }
}
