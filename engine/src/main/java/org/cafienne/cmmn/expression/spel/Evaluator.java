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

package org.cafienne.cmmn.expression.spel;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class Evaluator {
    private final ExpressionParser parser;
    private final Expression expression;
    private final String source;
    private final CMMNElementDefinition definition;

    public Evaluator(CMMNElementDefinition definition, String source) {
        this.parser = new SpelExpressionParser();
        this.definition = definition;
        this.source = source;
        this.expression = parseExpression();
    }

    public boolean isValid() {
        return expression != null;
    }

    public Expression getExpression() {
        return expression;
    }

    protected Expression parseExpression() {
        if (source.isBlank()) {
            // Empty expressions lead to IllegalStateException("no node") --> checking here gives a somewhat more understandable error ...
            definition.getModelDefinition().addDefinitionError(definition.getContextDescription() + " has no expression");
            return null;
        }

        try {
            return parser.parseExpression(source);
        } catch (Exception spe) { // Parser uses Assert class of Spring, which raises also exceptions like IllegalStateException
            definition.getModelDefinition().addDefinitionError(definition.getContextDescription() + " has an invalid expression:\n" + spe.getMessage());
            return null;
        }
    }

    public <T> T evaluate(APIRootObject<?> rootObject) {
        // System.out.println("Now evaluating the expression " + definition.getBody());
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
        // The property reader can dynamically resolve properties that belong to the ModelActor context.
        SpelReadableRecognizer spelPropertyReader = new SpelReadableRecognizer(rootObject.getActor());
        context.addPropertyAccessor(spelPropertyReader);

        return returnValue(rootObject, () -> expression.getValue(context));
    }

    protected <T> T returnValue(APIRootObject<?> rootObject, ExpressionRunner runner) {
        rootObject.getActor().addDebugInfo(() -> "Evaluating " + rootObject.getDescription() + ". Expression: " + source.trim());
        try {
            // Java has no support for typechecking the outcome on the generic T.
            //  Hence we add a log message with the actual return type and the value.
            //  Further down the thread of execution a check for typesafety can be done, otherwise a ClassCastExeption will pop up.
            @SuppressWarnings("unchecked")
            T value = (T) runner.calculate();
            String valueType = value == null ? "NULL" : value.getClass().getSimpleName();
            rootObject.getActor().addDebugInfo(() -> "Outcome has type " + valueType + " and value: " + value);
            return value;
        } catch (EvaluationException invalidExpression) {
            rootObject.getActor().addDebugInfo(() -> "Failure in evaluating " + rootObject.getDescription() + ", with expression " + source.trim(), invalidExpression);
            throw new InvalidExpressionException("Could not evaluate " + expression.getExpressionString() + "\n" + invalidExpression.getLocalizedMessage(), invalidExpression);
        } catch (Exception exception) {
            rootObject.getActor().addDebugInfo(() -> "Failure in evaluating " + rootObject.getDescription() + ", with expression " + source.trim(), exception);
            throw new InvalidExpressionException("Error during evaluation of " + expression.getExpressionString() + "\n" + exception.getLocalizedMessage(), exception);
        }
    }

    @FunctionalInterface
    public interface ExpressionRunner {
        Object calculate();
    }
}
