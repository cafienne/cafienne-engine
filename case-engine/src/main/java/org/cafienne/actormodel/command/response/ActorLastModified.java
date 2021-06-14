/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.command.response;

import org.cafienne.cmmn.actorapi.response.InvalidCaseLastModifiedException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

public class ActorLastModified {
    private final static String SEPARATOR = ";";

    //Variable to store the command completed moment
    private final Instant lastModified;
    //Variable to store the case instance identifier
    private final String actorId;
    private final String string;

    public ActorLastModified(String actorId, Instant lastModified) {
        this.lastModified = lastModified;
        this.actorId = actorId;
        this.string = asString();
    }

    private String asString() {
        return lastModified == null ? null : lastModified + SEPARATOR + actorId;
    }

    public ActorLastModified(String lastModifiedContent) throws InvalidCaseLastModifiedException {
        try {
            String[] lastModifiedHeaderParts = lastModifiedContent.split(SEPARATOR);
            this.lastModified = java.time.Instant.parse(lastModifiedHeaderParts[0]);
            this.actorId = lastModifiedHeaderParts[1];
            this.string = asString();
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
        return string;
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
