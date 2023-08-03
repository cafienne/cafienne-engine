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

package org.cafienne.cmmn.definition;

import org.cafienne.processtask.definition.ProcessDefinition;

/**
 * Basic element for definitions
 */
public interface DefinitionElement {
    String getId();

    String getName();

    /**
     * Returns the "type" of the element, which can be using in context descriptions.
     * Defaults to the simple class name, and if that ends with "Definition", this is removed.
     */
    default String getType() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Definition")) {
            return simpleName.substring(0, simpleName.length() - "Definition".length());
        } else {
            return simpleName;
        }
    }

    /**
     * Returns a description of the context this element provides to it's children. Can be used e.g. in expressions or on parts
     * to get the description of the parent element when encountering validation errors.
     */
    default String getContextDescription() {
        return "";
    }

    /**
     * Returns the model definition to which this element belongs
     */
    ModelDefinition getModelDefinition();

    /**
     * Cast of getModelDefinition to a CaseDefinition
     */
    default CaseDefinition getCaseDefinition() {
        return (CaseDefinition) getModelDefinition();
    }

    /**
     * Cast of getModelDefinition to a ProcessDefinition
     */
    default ProcessDefinition getProcessDefinition() {
        return (ProcessDefinition) getModelDefinition();
    }

    /**
     * Indication that this definition element differs from another element.
     * Used during migration of definitions of an instance of that definition.
     */
    boolean differs(Object object);
}
