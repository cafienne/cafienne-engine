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

package org.cafienne.cmmn.definition.casefile;

import org.cafienne.cmmn.definition.casefile.definitiontype.*;
import org.cafienne.json.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of CMMN spec 5.1.4
 */
public class DefinitionType {
    private final static Logger logger = LoggerFactory.getLogger(DefinitionType.class);

    public static DefinitionType resolveDefinitionType(String uri) {
        if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/CMISFolder")) {
            // Not implemented
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/CMISDocument")) {
            // Not implemented
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/CMISRelationship")) {
            // Not implemented
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/XSDElement")) {
            return new XMLElementType();
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/XSDComplexType")) {
            return new XMLComplexType();
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/XSDSimpleType")) {
            return new XMLSimpleType();
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/WSDLMessage")) {
            // Not implemented
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/UMLClass")) {
            // Not implemented
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/Unknown")) {
            return new UnknownType();
        } else if (uri.equals("http://www.omg.org/spec/CMMN/DefinitionType/Unspecified")) {
            return new UnspecifiedType();
        }
        logger.debug("Creating UnknownType wrapper for unrecognized type " + uri);
        return new UnknownType(uri);
    }

    /**
     * Validate the given value for the case file item. DefinitionType may use the case file item's definition
     * to determine whether the value can be set within the case file item.
     * The default implementation is empty - anything goes. Subclasses can override this to do type specific validations.
     *
     * @param item
     * @param value
     */
    public void validate(CaseFileItemDefinition item, Value<?> value) throws CaseFileError {
    }
}
