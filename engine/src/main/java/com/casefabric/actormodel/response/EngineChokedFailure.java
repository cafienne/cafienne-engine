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

package com.casefabric.actormodel.response;

import com.casefabric.actormodel.command.ModelCommand;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Can be used to return an exception to the sender of the command when the engine ran into some non-functional exception.
 */
@Manifest
public class EngineChokedFailure extends CommandFailure {
    /**
     * Create a failure response for the command.
     * The message id of the command will be pasted into the message id of the response.
     *
     * @param command
     * @param failure The reason why the command failed
     */
    public EngineChokedFailure(ModelCommand command, Throwable failure) {
        super(command, failure);
    }

    public EngineChokedFailure(ValueMap json) {
        super(json);
    }
}
