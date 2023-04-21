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
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class WriteColumnDefinitionEvent extends ColumnDefinitionEvent {
    public final scala.Option<String> title;
    public final scala.Option<String> role;
    public final scala.Option<ValueMap> form;

    protected WriteColumnDefinitionEvent(BoardActor board, String columnId, scala.Option<String> title, scala.Option<String> role, scala.Option<ValueMap> form) {
        super(board, columnId);
        this.title = title;
        this.role = role;
        this.form = form;
    }

    protected WriteColumnDefinitionEvent(ValueMap json) {
        super(json);
        this.title = json.readOption(Fields.title);
        this.role = json.readOption(Fields.role);
        this.form = json.readOptionalMap(Fields.form);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeColumnDefinitionEvent(generator);
        writeField(generator, Fields.title, title);
        writeField(generator, Fields.role, role);
        writeField(generator, Fields.form, form);
    }
}
