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

package org.cafienne.actormodel.response;

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
