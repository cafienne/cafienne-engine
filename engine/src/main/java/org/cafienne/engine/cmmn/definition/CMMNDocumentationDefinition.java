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

import org.w3c.dom.Element;

/**
 * Basic implementation of &lt;cmmn:documentation&gt; element
 */
public class CMMNDocumentationDefinition extends XMLElementDefinition {
    public final String textFormat;
    public final String text;

    public CMMNDocumentationDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.textFormat = parseAttribute("textFormat", false, "text/plain");
        this.text = parseString("text", false, "");
    }

    /**
     * Constructor used to create an instance if the documentation node does not exist in the definition.
     * Also converts a potential CMMN1.0 description into the new documentation element.
     * Note: this does not modify the underlying XML!
     *
     * @param definition
     * @param parentElement
     */
    CMMNDocumentationDefinition(ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(null, definition, parentElement);
        this.textFormat = "text/plain";
        this.text = parentElement.parseAttribute("description", false, "");
    }

    /**
     * Returns text format
     *
     * @return
     */
    public String getTextFormat() {
        return textFormat;
    }

    /**
     * Returns text
     *
     * @return
     */
    public String getText() {
        return text;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameDocumentation);
    }

    public boolean sameDocumentation(CMMNDocumentationDefinition other) {
        return same(textFormat, other.textFormat)
                && same(text, other.text);
    }
}
