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

package org.cafienne.board.actorapi.command.flow;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.event.flow.FlowInitiated;
import org.cafienne.board.actorapi.response.FlowStartedResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class StartFlow extends BoardFlowCommand {
    public final String flowId;
    public final String subject;
    public final ValueMap data;

    public StartFlow(BoardUser user, String flowId, String subject, ValueMap data) {
        super(user);
        this.flowId = flowId;
        this.subject = subject;
        this.data = data;
    }

    public StartFlow(ValueMap json) {
        super(json);
        this.flowId = json.readString(Fields.flowId);
        this.subject = json.readString(Fields.subject);
        this.data = json.readMap(Fields.input);
    }

    @Override
    public void process(BoardActor board) {
        board.addEvent(new FlowInitiated(board, flowId, subject, data));
        setResponse(new FlowStartedResponse(this, flowId));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.flowId, flowId);
        writeField(generator, Fields.subject, subject);
        writeField(generator, Fields.input, data);
    }
}

