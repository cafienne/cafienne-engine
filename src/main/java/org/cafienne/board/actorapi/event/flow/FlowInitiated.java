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

package org.cafienne.board.actorapi.event.flow;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.event.BoardEvent;
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class FlowInitiated extends BoardFlowEvent {
    public final String flowId;
    public final String subject;
    public final ValueMap input;

    public FlowInitiated(BoardActor board, String flowId, String subject, ValueMap input) {
        super(board);
        this.flowId = flowId;
        this.subject = subject;
        this.input = input;
    }

    public FlowInitiated(ValueMap json) {
        super(json);
        this.flowId = json.readString(Fields.flowId);
        this.subject = json.readString(Fields.subject);
        this.input = json.readMap(Fields.input);
    }

    @Override
    public void updateState(BoardActor actor) {
        actor.startFlow(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeBoardEvent(generator);
        writeField(generator, Fields.flowId, flowId);
        writeField(generator, Fields.subject, subject);
        writeField(generator, Fields.input, input);
    }
}
