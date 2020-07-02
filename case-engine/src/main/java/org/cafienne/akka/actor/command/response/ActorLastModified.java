/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor.command.response;

import org.cafienne.cmmn.akka.command.response.InvalidCaseLastModifiedException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class ActorLastModified {

    //Variable to store the command completed moment
    private final Instant lastModified;
    //Variable to store the case instance identifier
    private final String actorId;

    public ActorLastModified(String actorId, Instant lastModified) {
        this.lastModified = lastModified;
        this.actorId = actorId;
    }

    public ActorLastModified(String lastModifiedContent) throws InvalidCaseLastModifiedException {
        try {
            String[] lastModifiedHeaderParts = lastModifiedContent.split(";");
            this.lastModified = java.time.Instant.parse(lastModifiedHeaderParts[0]);
            this.actorId = lastModifiedHeaderParts[1];
        } catch (ArrayIndexOutOfBoundsException | DateTimeParseException | NullPointerException ex) {
            if (ex instanceof DateTimeParseException) {
                throw new InvalidCaseLastModifiedException("Invalid time stamp '" + ((DateTimeParseException) ex).getParsedString() + "' received in CaseLastModified header");
            } else {
                throw new InvalidCaseLastModifiedException("Provide a valid CaseLastModified header");
            }
        }
    }

    @Override
    public String toString() {
        return lastModified.toString() + ";" + actorId;
    }

    /**
     * Returns last modified
     *
     * @return Instant
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Returns case instance identifier
     *
     * @return String
     */
    public String getCaseInstanceId() {
        return actorId;
    }

}
