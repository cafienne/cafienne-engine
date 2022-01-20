/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.response;

import akka.actor.ActorRef;
import org.cafienne.actormodel.IncomingActorMessage;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

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
     * Returns the actor ref to whom the response message must be sent.
     */
    ActorRef getRecipient();

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
}
