/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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