/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.EventListener;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

public abstract class EventListenerDefinition extends PlanItemDefinitionDefinition {
    protected EventListenerDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public abstract EventListener<?> createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance);
}
