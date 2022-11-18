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

package org.cafienne.processtask.implementation.http.definition;

import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.net.MalformedURLException;
import java.net.URL;

public class URLDefinition extends SubProcessInputMappingDefinition {
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
