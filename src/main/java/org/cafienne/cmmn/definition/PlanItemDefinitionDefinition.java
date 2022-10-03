/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

public abstract class PlanItemDefinitionDefinition extends CMMNElementDefinition {
    private ItemControlDefinition defaultControl;

    public PlanItemDefinitionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
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
