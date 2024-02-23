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

import java.util.Collection;

/**
 * Basic element for definitions
 */
public interface DefinitionElement {
    String getId();

    String getName();

    /**
     * Returns whether the parent of this Element can have only one occurrence of this type of DefinitionElement.
     *
     * @return Defaults to false, override it by implementing SingletonDefinitionElement interface.
     */
    default boolean isSingletonElement() {
        return false;
    }

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
     * Custom compare method. Comparable to Object.equals(), but elements are expected
     * to implement a semantic comparison.
     */
    boolean differs(DefinitionElement object);

    /**
     * Returns true if this element has the identifier in its name or id attribute.
     */
    default boolean hasIdentifier(String identifier) {
        return this.getId().equals(identifier) || this.getName().equals(identifier);
    }

    /**
     * Returns true if the other DefinitionElement has the same name or the same id.
     */
    default boolean hasMatchingIdentifier(DefinitionElement other) {
        return this.getId().equals(other.getId()) || this.getName().equals(other.getName());
    }

    /**
     * Find an equal definition in the collection, using the equalsWith method.
     *
     * @param mine   The element that we want to find an alternative for
     * @param theirs The collection to search the element
     * @param <T>    Target type to cast to
     * @param <Z>    Base type to compare on, to help also search in generics based collections (e.g. Collection[OnPartDefinition])
     * @return null if the element was not found in the collection
     */
    static <T extends Z, Z extends DefinitionElement> T findDefinition(T mine, Collection<Z> theirs) {
        for (Z his : theirs) {
            if (mine.hasMatchingIdentifier(his)) {
                return (T) his; // Cast is ok, because it is checked inside the similarDefinition method to be the same class.
            }
        }
        return null;
    }
}
