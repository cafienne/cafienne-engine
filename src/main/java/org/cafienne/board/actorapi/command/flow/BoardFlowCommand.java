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

    public void writeFlowCommand(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.flowId, flowId);
    }
}
