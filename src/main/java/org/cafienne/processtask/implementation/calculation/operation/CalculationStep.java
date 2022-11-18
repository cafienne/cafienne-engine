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
