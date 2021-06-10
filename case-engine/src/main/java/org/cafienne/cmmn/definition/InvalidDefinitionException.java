/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.actormodel.serialization.json.Value;
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
        this.definitionErrors = new ArrayList();
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
     * @return
     */
    public Value<?> toJSON() {
        return Value.convert(definitionErrors);
    }

    /**
     * Returns an XML Document &lt;invalid-definition&gt; with error tags inside
     * for each error found in the definitions document
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
