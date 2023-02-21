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
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.response.BoardResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class CompleteFlowTask extends BoardFlowCommand {
    public final String taskId;
    public final String subject;
    public final ValueMap data;

    public CompleteFlowTask(BoardUser user, String flowId, String taskId, String subject, ValueMap data) {
        super(user, flowId);
        this.taskId = taskId;
        this.subject = subject;
        this.data = data;
    }

    public CompleteFlowTask(ValueMap json) {
        super(json);
        this.taskId = json.readString(Fields.taskId);
        this.subject = json.readString(Fields.subject);
        this.data = json.readMap(Fields.output);
    }

    @Override
    public void validate(BoardActor board) throws InvalidCommandException {
        super.validate(board);
        if (board.state.flows().get(flowId).isEmpty()) {
            // TODO: come up with a good exception report that is secure ...
            throw new InvalidCommandException("This flow does not exist");
        }
    }

    @Override
    public BoardResponse process(BoardActor board) {
        board.state.flows().get(flowId).get().completeTask(getUser(), taskId, subject, data);
        return new BoardResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeFlowCommand(generator);
        writeField(generator, Fields.taskId, taskId);
        writeField(generator, Fields.subject, subject);
        writeField(generator, Fields.output, data);
    }
}

