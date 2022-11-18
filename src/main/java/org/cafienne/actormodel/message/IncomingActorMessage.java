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

package org.cafienne.actormodel.message;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.response.ModelResponse;

/**
 * An IncomingActorMessage is received by a ModelActor. Typically a ModelCommand or a ModelResponse.
 * It may lead to state changes in the actor
 */
public interface IncomingActorMessage extends UserMessage {
    /**
     * Every message must have a unique identifier. This can be used to correlate Commands and Responses.
     *
     * @return
     */
    String getMessageId();

    default boolean isCommand() {
        return false;
    }

    default boolean isResponse() {
        return false;
    }

    default ModelCommand asCommand() {
        return null;
    }

    default ModelResponse asResponse() {
        return null;
    }
}
