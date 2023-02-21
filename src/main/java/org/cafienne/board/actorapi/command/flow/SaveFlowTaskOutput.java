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

import org.cafienne.actormodel.identity.BoardUser;
import org.cafienne.board.state.FlowState;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class SaveFlowTaskOutput extends FlowTaskOutputCommand {
    public SaveFlowTaskOutput(BoardUser user, String flowId, String taskId, String subject, ValueMap data) {
        super(user, flowId, taskId, subject, data);
    }

    public SaveFlowTaskOutput(ValueMap json) {
        super(json);
    }

    @Override
    public void process(FlowState flow) {
        flow.saveTask(getUser(), taskId, subject, data);
    }
}

