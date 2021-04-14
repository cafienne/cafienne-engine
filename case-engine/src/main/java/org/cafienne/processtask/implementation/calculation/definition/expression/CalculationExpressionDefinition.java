/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.definition.expression;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.ExpressionEvaluator;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.w3c.dom.Element;

public class CalculationExpressionDefinition extends CMMNElementDefinition {
    private final ExpressionEvaluator evaluator;
    private final String expression;

    public CalculationExpressionDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.expression = parseString(null, false, "");
        this.evaluator = new ExpressionEvaluator(this);
    }

    public Result getResult(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
        // If there is an expression, execute it on the incoming values, otherwise just return the incoming values
        if (expression.isEmpty()) {
            return new Result(calculation, step, sourceMap);
        } else {
            return new Result(calculation, step, evaluateExpression(calculation, step, sourceMap));
        }
    }

    protected Value evaluateExpression(Calculation calculation, CalculationStep step, ValueMap sourceMap) {
        return evaluator.runCalculationStep(calculation, step, sourceMap);
    }

    public String getExpression() {
        return expression;
    }

    public String getType() {
        return "Step";
    }
}
