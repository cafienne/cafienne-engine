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
    private final ConstraintDefinition requiredRule;
    private final ConstraintDefinition repetitionRule;
    private final ConstraintDefinition manualActivationRule;

    ItemControlDefinition(ModelDefinition definition, CMMNElementDefinition parentElement) {
        // TODO: it is better to parse a default XML element?
        //  - but that has more serious impact, since the null element inside the ExpressionDefinition
        //  and the defaultValue is used to decide to create a DefaultValueEvaluator, and
        // ... that is used to determine inside the plan item whether a repetitionRule is default or not
        // ... and that info is used ... in quite a few places (4 or 5)
        this(null, definition, parentElement);
    }

    public ItemControlDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        repetitionRule = parseRule(element, "repetitionRule", false);
        requiredRule = parseRule(element, "requiredRule", false);
        manualActivationRule = parseRule(element, "manualActivationRule", false);
    }

    private ConstraintDefinition parseRule(Element element, String type, boolean defaultValue) {
        ConstraintDefinition rule = null;
        if (element != null) {
            rule = parse(type, ConstraintDefinition.class, false);
        }
        if (rule == null) { // Create a default rule
            rule = new ConstraintDefinition(getModelDefinition(), this, type, defaultValue);
        }
        return rule;
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
