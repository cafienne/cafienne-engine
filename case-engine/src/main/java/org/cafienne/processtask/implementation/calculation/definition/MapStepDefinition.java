/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.definition.expression.MapExpressionDefinition;
import org.w3c.dom.Element;

public class MapStepDefinition extends StepDefinition {
    public MapStepDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement, MapExpressionDefinition.class);
    }

    @Override
    public String getType() {
        return "Mapping step";
    }

    @Override
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
