/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

public abstract class PlanItemDefinitionDefinition extends CMMNElementDefinition {
    private ItemControlDefinition defaultControl;

    public PlanItemDefinitionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        defaultControl = parse("defaultControl", ItemControlDefinition.class, false);
        if (defaultControl == null) {
            // Make an item control with the default values
            defaultControl = new ItemControlDefinition(modelDefinition, parentElement);
        }
    }

    public ItemControlDefinition getDefaultControl() {
        return defaultControl;
    }

    public abstract PlanItem createInstance(String id, int index, ItemDefinition itemDefinition, Stage stage, Case caseInstance);

    /**
     * Returns the transition that is to be invoked on the plan item when one of the entry criteria sentries is satisfied
     *
     * @return
     */
    public abstract Transition getEntryTransition();
}
