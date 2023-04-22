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
import org.cafienne.board.state.definition.BoardDefinition;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class WriteColumnDefinitionEvent extends ColumnDefinitionEvent {
    public final String title;
    public final String role;
    public final ValueMap form;

    protected WriteColumnDefinitionEvent(BoardDefinition board, String columnId, String title, String role, ValueMap form) {
        super(board, columnId);
        this.title = title;
        this.role = role;
        this.form = form;
    }

    protected WriteColumnDefinitionEvent(ValueMap json) {
        super(json);
        this.title = json.readString(Fields.title);
        this.role = json.readString(Fields.role);
        this.form = json.readMap(Fields.form);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeColumnDefinitionEvent(generator);
        writeField(generator, Fields.title, title);
        writeField(generator, Fields.role, role);
        writeField(generator, Fields.form, form);
    }
}
