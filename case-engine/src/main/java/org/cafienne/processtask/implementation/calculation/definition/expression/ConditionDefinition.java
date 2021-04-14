/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.akka.actor.serialization.json.BooleanValue;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

public class ConditionDefinition extends CalculationExpressionDefinition {
    public ConditionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }

    public boolean getBooleanResult(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
        Value validity = super.evaluateExpression(calculation, step, sourceMap);
        if (validity instanceof BooleanValue) {
            return ((BooleanValue) validity).getValue();
        } else {
            // Now what?!
            calculation.getTask().getCaseInstance().addDebugInfo(() -> getType() + " expression in " + step.getDefinition().getDescription() + " does not return a boolean value. Returning false");
            return false;
        }
    }

    @Override
    public String getType() {
        return "Condition";
    }
}
