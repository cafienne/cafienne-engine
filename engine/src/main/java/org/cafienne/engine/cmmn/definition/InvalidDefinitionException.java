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

package org.cafienne.engine.cmmn.definition;

import org.cafienne.json.Value;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Exception thrown upon parsing an invalid {@link DefinitionsDocument}
 */
public class InvalidDefinitionException extends Exception {
    private final Collection<String> definitionErrors;

    InvalidDefinitionException(Collection<String> definitionErrors) {
        super("Invalid case definition: " + definitionErrors);
        this.definitionErrors = definitionErrors;
    }

    public InvalidDefinitionException(String msg, Throwable t) {
        super(msg, t);
        this.definitionErrors = new ArrayList<>();
        definitionErrors.add(msg);
        definitionErrors.add("details: " + t.toString());
    }

    public String toString() {
        return "Invalid definition: " + definitionErrors;
    }

    public Collection<String> getErrors() {
        return definitionErrors;
    }

    /**
     * Returns a ValueList of the errors, basically a json array.
     *
     * @return
     */
    public Value<?> toJSON() {
        return Value.convert(definitionErrors);
    }

    /**
     * Returns an XML Document &lt;invalid-definition&gt; with error tags inside
     * for each error found in the definitions document
     *
     * @return
     */
    public Document toXML() {
        try {
            Document doc = XMLHelper.loadXML("<invalid-definition xmlns=\"org.cafienne\"/>");
            definitionErrors.forEach(error -> {
                Element errorTag = (Element) doc.getDocumentElement().appendChild(doc.createElement("error"));
                errorTag.appendChild(doc.createTextNode(error));
            });
            return doc;
        } catch (IOException | ParserConfigurationException | SAXException thisNeverHappens) {
            thisNeverHappens.printStackTrace();// It did happen ?!
            return null;
        }
    }
}
