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
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.board.BoardActor;
import org.cafienne.board.actorapi.command.BoardCommand;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Base class for sending commands to a TenantActor
 */
public abstract class BoardFlowCommand extends BoardCommand {
    public final String flowId;

    /**
     * Create a new command that can be sent to the board.
     *
     * @param user The user that issues this command.
     */
    protected BoardFlowCommand(BoardUser user, String flowId) {
        super(user);
        this.flowId = flowId;
    }

    protected BoardFlowCommand(ValueMap json) {
        super(json);
        this.flowId = json.readString(Fields.flowId);
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
    public void process(BoardActor board) {
        board.state.flows().get(flowId).get().handle(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeFlowCommand(generator);
    }

    public void writeFlowCommand(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.flowId, flowId);
    }
}
