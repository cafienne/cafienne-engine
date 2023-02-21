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
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

public abstract class FlowTaskOutputCommand extends FlowTaskCommand {
    public final String subject;
    public final ValueMap data;

    protected FlowTaskOutputCommand(BoardUser user, String flowId, String taskId, String subject, ValueMap data) {
        super(user, flowId, taskId);
        this.subject = subject;
        this.data = data;
    }

    protected FlowTaskOutputCommand(ValueMap json) {
        super(json);
        this.subject = json.readString(Fields.subject);
        this.data = json.readMap(Fields.output);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeFlowTaskOutputCommand(generator);
    }

    protected void writeFlowTaskOutputCommand(JsonGenerator generator) throws IOException {
        super.writeFlowTaskCommand(generator);
        writeField(generator, Fields.subject, subject);
        writeField(generator, Fields.output, data);
    }
}

