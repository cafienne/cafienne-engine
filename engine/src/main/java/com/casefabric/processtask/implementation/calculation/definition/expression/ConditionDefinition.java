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

package com.casefabric.processtask.implementation.calculation.definition.expression;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.json.BooleanValue;
import com.casefabric.json.Value;
import com.casefabric.processtask.implementation.calculation.Calculation;
import com.casefabric.processtask.implementation.calculation.definition.source.InputReference;
import com.casefabric.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

import java.util.Map;

public class ConditionDefinition extends CalculationExpressionDefinition {
    public ConditionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }

    public boolean getBooleanResult(Calculation calculation, CalculationStep step, Map<InputReference, Value<?>> sourceMap) {
        Value<?> validity = super.evaluateExpression(calculation, step, sourceMap);
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

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
