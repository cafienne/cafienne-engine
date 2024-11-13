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

package com.casefabric.cmmn.definition.casefile;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class CaseFileItemCollectionDefinition extends CMMNElementDefinition {
    private final Collection<CaseFileItemDefinition> items = new ArrayList<>();

    public CaseFileItemCollectionDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
    }

    public Collection<CaseFileItemDefinition> getChildren() {
        return items;
    }

    public CaseFileItemDefinition getChild(String identifier) {
        return getChildren().stream().filter(i -> i.hasIdentifier(identifier)).findFirst().orElse(null);
    }

    /**
     * Returns true if an item with the identifier does not exist
     *
     * @param identifier
     * @return
     */
    public boolean isUndefined(String identifier) {
        return getChild(identifier) == null;
    }

    public boolean contains(CaseFileItemCollectionDefinition potentialChild) {
        if (potentialChild == null) {
            return false;
        } else if (potentialChild == this) {
            return true;
        } else {
            return contains(potentialChild.getParentElement());
        }
    }

    /**
     * Recursively searches this level and all children until an item with the specified name is found.
     *
     * @param identifier
     * @return
     */
    public CaseFileItemDefinition findCaseFileItem(String identifier) {
        CaseFileItemDefinition item = getChild(identifier);
        if (item == null) {
            for (CaseFileItemDefinition caseFileItem : getChildren()) {
                item = caseFileItem.findCaseFileItem(identifier);
                if (item != null) {
                    // Immediately return if we found one.
                    return item;
                }
            }
        }
        return item;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCollection);
    }

    public boolean sameCollection(CaseFileItemCollectionDefinition other) {
        return sameIdentifiers(other)
                && same(items, other.items);
    }
}
