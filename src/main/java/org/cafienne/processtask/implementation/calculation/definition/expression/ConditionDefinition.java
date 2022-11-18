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

package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.json.BooleanValue;
import org.cafienne.json.Value;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
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
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
