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
import com.casefabric.cmmn.expression.spel.ExpressionEvaluator;
import com.casefabric.json.Value;
import com.casefabric.processtask.implementation.calculation.Calculation;
import com.casefabric.processtask.implementation.calculation.Result;
import com.casefabric.processtask.implementation.calculation.definition.source.InputReference;
import com.casefabric.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

import java.util.Map;

public class CalculationExpressionDefinition extends CMMNElementDefinition {
    private final ExpressionEvaluator evaluator;
    private final String expression;

    public CalculationExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.expression = parseString(null, false, "");
        this.evaluator = new ExpressionEvaluator(this);
    }

    public Result getResult(Calculation calculation, CalculationStep step, Map<InputReference, Value<?>> sourceMap) {
        // If there is an expression, execute it on the incoming values, otherwise just return the incoming values
        return new Result(calculation, step, evaluateExpression(calculation, step, sourceMap));
    }

    protected Value<?> evaluateExpression(Calculation calculation, CalculationStep step, Map<InputReference, Value<?>> sourceMap) {
        return evaluator.runCalculationStep(calculation, step, sourceMap);
    }

    public String getExpression() {
        return expression;
    }

    public String getType() {
        return "Step";
    }

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
