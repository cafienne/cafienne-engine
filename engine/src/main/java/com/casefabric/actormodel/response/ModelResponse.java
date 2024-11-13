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
import com.casefabric.actormodel.message.IncomingActorMessage;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

import java.time.Instant;

/**
 * Interface for creating responses to {@link ModelCommand}
 */
public interface ModelResponse extends IncomingActorMessage {
    /**
     * Set the last modified timestamp of the ModelActor.
     */
    void setLastModified(Instant lastModified);

    /**
     * Return the last modified timestamp of the ModelActor, along with the actor id.
     */
    ActorLastModified lastModifiedContent();

    /**
     * Return a Value representation of the response content.
     * Defaults to an empty json object.
     */
    default Value<?> toJson() {
        return new ValueMap();
    }

    @Override
    default boolean isResponse() {
        return true;
    }

    @Override
    default ModelResponse asResponse() {
        return this;
    }
}
