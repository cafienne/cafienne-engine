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

package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class ItemDefinitionReference extends CMMNElementDefinition {
    private final String itemRef;
    private final String itemName;
    private ItemDefinition itemDefinition;

    public ItemDefinitionReference(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.itemRef = parseAttribute("taskRef", true);
        this.itemName = parseAttribute("taskName", false, "");
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        this.itemDefinition = getCaseDefinition().getElement(itemRef);
        if (this.itemDefinition == null) {
            getCaseDefinition().addReferenceError("Cannot find a task with reference " + itemRef +" and name '" + itemName + "'");
        }
    }

    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameItemReference);
    }

    private boolean sameItemReference(ItemDefinitionReference other) {
        return same(this.itemRef, other.itemRef) && same(this.itemName, other.itemName);
    }
}
