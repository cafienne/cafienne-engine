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

import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

public abstract class PlanItemDefinitionDefinition extends CMMNElementDefinition {
    private ItemControlDefinition defaultControl;

    public PlanItemDefinitionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        if (this.getName().isEmpty()) {
            modelDefinition.addDefinitionError("The " + getItemType() + " with id " + this.getId() +" must have a name");
        }

        defaultControl = parse("defaultControl", ItemControlDefinition.class, false);
        if (defaultControl == null) {
            // Make an item control with the default values
            defaultControl = new ItemControlDefinition(getModelDefinition(), this);
        }
    }

    /**
     * Returns the PlanItemDefinition or DiscretionaryItemDefinition that is associated with this PIDD.
     * Note: in the spec it is possible to re-use the same PIDD across multiple PID or DID, but in our tooling
     * we do not support that. As a matter of fact, no tooling supports that, because it cannot be visualized at all.
     * We consider it a "bug" in the spec, and are still hoping for a use case that proves the opposite ;)
     */
    public ItemDefinition findItemDefinition() {
        return getCaseDefinition().findElement(element -> element instanceof ItemDefinition && ((ItemDefinition) element).getPlanItemDefinition() == this);
    }

    public abstract PlanItemType getItemType();

    public ItemControlDefinition getDefaultControl() {
        return defaultControl;
    }

    public abstract PlanItem<?> createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance);

    /**
     * Returns the transition that is to be invoked on the plan item when one of the entry criteria sentries is satisfied
     *
     * @return
     */
    public abstract Transition getEntryTransition();

    protected boolean sameItemControl(PlanItemDefinitionDefinition other) {
        return same(defaultControl, other.defaultControl);
    }

    public boolean samePlanItemDefinitionDefinition(PlanItemDefinitionDefinition other) {
        return sameIdentifiers(other)
                && sameItemControl(other);
    }
}
