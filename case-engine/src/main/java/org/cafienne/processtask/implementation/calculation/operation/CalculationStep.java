/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.operation;

import org.cafienne.json.Value;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.expression.ConditionDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;

import java.util.HashMap;
import java.util.Map;

public class CalculationStep extends Source<StepDefinition> {
    private Map<InputReference, Value<?>> inputs = null;
    private final ConditionDefinition condition;

    public CalculationStep(Calculation calculation, StepDefinition definition) {
        super(definition, calculation);
        this.condition = definition.getCondition();
    }

    protected Map<InputReference, Value<?>> getInputs() {
        if (inputs == null) {
            inputs = new HashMap<>();
            for (InputReference input : definition.getInputs()) {
                Source<?> source = calculation.getSource(input.getSource());
                inputs.put(input, source.getResult().getValue());
            }
        }
        return inputs;
    }

    public boolean isValid() {
        if (condition == null) {
            return true;
        }
        return condition.getBooleanResult(calculation, this, getInputs());
    }

    protected Result calculateResult() {
        // Let the expression create a result for us
        return definition.getResult(calculation, this, getInputs());
    }
}
