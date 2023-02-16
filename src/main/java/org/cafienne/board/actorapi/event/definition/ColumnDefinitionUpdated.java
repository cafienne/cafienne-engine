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
public class ColumnDefinitionUpdated extends BoardDefinitionEvent {
    public final String columnId;
    public final scala.Option<String> title;
    public final scala.Option<ValueMap> form;

    public ColumnDefinitionUpdated(BoardActor board, String columnId, scala.Option<String> title, scala.Option<ValueMap> form) {
        super(board);
        this.columnId = columnId;
        this.title = title;
        this.form = form;
    }

    public ColumnDefinitionUpdated(ValueMap json) {
        super(json);
        this.columnId = json.readString(Fields.columnId);
        this.title = json.readOption(Fields.title);
        this.form = json.readOptionalMap(Fields.form);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeBoardEvent(generator);
        writeField(generator, Fields.columnId, columnId);
        writeField(generator, Fields.title, title);
        writeField(generator, Fields.form, form);
    }
}
