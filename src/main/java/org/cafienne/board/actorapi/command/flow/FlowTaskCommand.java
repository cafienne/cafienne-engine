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
import org.cafienne.board.state.FlowState;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class FlowTaskCommand extends BoardFlowCommand {
    public final String taskId;

    protected FlowTaskCommand(BoardUser user, String flowId, String taskId) {
        super(user, flowId);
        this.taskId = taskId;
    }

    protected FlowTaskCommand(ValueMap json) {
        super(json);
        this.taskId = json.readString(Fields.taskId);
    }

    @Override
    public void validate(BoardActor board) throws InvalidCommandException {
        super.validate(board);
        if (board.state.flows().get(flowId).isEmpty()) {
            // TODO: come up with a good exception report that is secure ...
            throw new InvalidCommandException(this.getClass().getSimpleName() + " cannot be performed: the flow does not exist");
        }
    }

    @Override
    public BoardResponse process(BoardActor board) {
        process(board.state.flows().get(flowId).get());
        return new BoardResponse(this);
    }

    protected abstract void process(FlowState flow);

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeFlowTaskCommand(generator);
    }

    public void writeFlowTaskCommand(JsonGenerator generator) throws IOException {
        super.writeFlowCommand(generator);
        writeField(generator, Fields.taskId, taskId);
    }
}

