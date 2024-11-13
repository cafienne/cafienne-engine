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

package com.casefabric.processtask.implementation.http.definition;

import com.casefabric.actormodel.exception.CommandException;
import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.expression.spel.api.APIRootObject;
import com.casefabric.processtask.definition.ProcessInputResolver;
import com.casefabric.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URL;

public class URLDefinition extends ProcessInputResolver {
    public URLDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    public URL resolveURL(APIRootObject<?> context) {
        String finalURL = resolve(context);
        return parseURL(finalURL);
    }

    public URL resolveURL(ProcessTaskActor task) {
        String finalURL = super.resolve(task);
        return parseURL(finalURL);
    }

    private URL parseURL(String finalURL) {
        try {
//            System.out.println("Resolved source url " + getSource() +" to " + finalURL);
            return new URL(finalURL);
        } catch (MalformedURLException e) {
            // TODO: this should throw a proper, catchable error
            throw new CommandException("\nInvalid URL binding. Source url: " + getSource() + "\nBinding result: " + finalURL, e);
        }
    }
}
