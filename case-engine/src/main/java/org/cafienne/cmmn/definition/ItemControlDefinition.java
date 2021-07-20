/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.w3c.dom.Element;

public class ItemControlDefinition extends CMMNElementDefinition {
    private ConstraintDefinition requiredRule;
    private ConstraintDefinition repetitionRule;
    private ConstraintDefinition manualActivationRule;

    ItemControlDefinition(ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(null, definition, parentElement);
        repetitionRule = new ConstraintDefinition(definition, this, false);
        requiredRule = new ConstraintDefinition(definition, this, false);
        manualActivationRule = new ConstraintDefinition(definition, this, false);
    }

    public ItemControlDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        repetitionRule = parse("repetitionRule", ConstraintDefinition.class, false);
        if (repetitionRule == null) {
            repetitionRule = new ConstraintDefinition(modelDefinition, this, false);
        }
        requiredRule = parse("requiredRule", ConstraintDefinition.class, false);
        if (requiredRule == null) {
            requiredRule = new ConstraintDefinition(modelDefinition, this, false);
        }
        manualActivationRule = parse("manualActivationRule", ConstraintDefinition.class, false);
        if (manualActivationRule == null) {
            manualActivationRule = new ConstraintDefinition(modelDefinition, this, false);
        }
    }

    public ConstraintDefinition getRequiredRule() {
        return requiredRule;
    }

    public ConstraintDefinition getRepetitionRule() {
        return repetitionRule;
    }

    public ConstraintDefinition getManualActivationRule() {
        return manualActivationRule;
    }
}
