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

package org.cafienne.board.actorapi.response;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.response.ActorLastModified;
import org.cafienne.board.actorapi.command.definition.column.AddColumnDefinition;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class ColumnAddedResponse extends BoardResponseWithContent {
    public final String columnId;

    public ColumnAddedResponse(AddColumnDefinition command, String columnId, ActorLastModified lastModified) {
        super(command, lastModified);
        this.columnId = columnId;
    }

    public ColumnAddedResponse(ValueMap json) {
        super(json);
        this.columnId = json.readString(Fields.columnId);
    }

    @Override
    public Value<?> toJson() {
        return new ValueMap(Fields.columnId, columnId);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.columnId, columnId);
    }
}
