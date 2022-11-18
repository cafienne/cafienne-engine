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

package org.cafienne.util;

import org.apache.commons.text.StringSubstitutor;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.json.ValueMap;

import java.net.MalformedURLException;
import java.net.URL;

public class StringTemplate {

    private final String sourceString;
    private String result;
    private StringSubstitutor strSubstitutor;

    public StringTemplate(String string) {
        this.sourceString = string;
    }

    public String getResult() {
        if (result == null) {
            result = strSubstitutor.replace(sourceString);
        }
        return result;
    }

    @Override
    public String toString() {
        return getResult();
    }

    /**
     * Only works for URL type of content.
     *
     * @return
     */
    public URL toURL() {
        String finalURL = this.toString();
        try {
            URL url = new URL(finalURL);
            return url;
        } catch (MalformedURLException e) {
            // TODO: this should throw a proper, catchable error
            throw new CommandException("\nInvalid URL binding. Source url: " + sourceString + "\nBinding result: " + finalURL, e);
        }
    }

    public StringTemplate resolveParameters(ValueMap processInputParameters) {
        this.strSubstitutor = new StringSubstitutor(processInputParameters.getValue());
        return this;
    }
}
