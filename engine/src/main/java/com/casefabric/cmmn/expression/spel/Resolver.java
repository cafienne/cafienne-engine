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

package com.casefabric.cmmn.expression.spel;

import org.apache.commons.text.StringSubstitutor;
import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.expression.ResolverDefinition;
import com.casefabric.cmmn.expression.InvalidExpressionException;
import com.casefabric.cmmn.expression.spel.api.APIRootObject;
import com.casefabric.cmmn.expression.spel.api.process.InputMappingRoot;
import com.casefabric.processtask.instance.ProcessTaskActor;

import java.util.LinkedHashMap;

/**
 * Enables multi-expression evaluation of task input parameters on string content,
 * based on the apache commons text {@link StringSubstitutor}.
 */
public class Resolver extends CMMNElementDefinition {
    private final Action action;
    private final String source;
    private final ResolverDefinition definition;

    @FunctionalInterface
    private interface Action {
        Object evaluate(APIRootObject<?> rootObject);
    }

    public Resolver(ResolverDefinition definition, String source) {
        super(definition.getElement(), definition.getModelDefinition(), definition);
        this.definition = definition;
        this.source = source;
        this.action = parseSource();
    }

    private Action parseSource() {
        // Let StringSubstitutor parse the source string, and collect all expressions.
        // Then determine the number of expressions found, with 3 options:
        // 1. No expressions found --> then we can simply return the source string upon invocation
        // 2. Only one expression is found, and it is also the whole expression (trimming whitespace)
        //    In that case, the expression is evaluated, and the outcome is returned, regardless of the type.
        //    This can be used to e.g. evaluate an expression to a ValueMap and use that down the line
        // 3. Multiple expressions are found, or there is "string" information trailing around the expression.
        //    In that case, the outcome of evaluation is always concatenated to a string.
        final LinkedHashMap<String, Evaluator> parsedExpressions = new LinkedHashMap<>();
        new StringSubstitutor(expression -> {
            if (definition.getInputParameters().containsKey(expression)) {
                parsedExpressions.put(expression, new ParameterEvaluator(definition, expression));
                return expression;
            } else {
                Evaluator parsedExpression = new Evaluator(definition, expression);
                if (parsedExpression.isValid()) {
                    parsedExpressions.put(expression, parsedExpression);
                    return expression;
                } else {
                    return ""; // Error is added while parsing
                }
            }
        }).replace(source);

        if (parsedExpressions.isEmpty()) {
            return root -> source;
        } else if (parsedExpressions.size() == 1 && source.trim().startsWith(StringSubstitutor.DEFAULT_VAR_START) && source.trim().endsWith(StringSubstitutor.DEFAULT_VAR_END)) {
            Evaluator evaluator = parsedExpressions.values().toArray(new Evaluator[]{})[0];
//            System.out.println("Found static expression: " + evaluator.getExpression() + " in source " + source);
            return evaluator::evaluate;
        } else {
            // Actual resolver uses the parsed expressions, and always returns a string
            return root ->
                    new StringSubstitutor(expression -> {
//                        System.out.println("Resolving expression [" + expression + "] in source " + source);
                        final Evaluator evaluator = parsedExpressions.get(expression);
                        if (evaluator == null) {
                            // This is really weird, since we have successfully parsed the expression upon earlier case validation.
                            throw new InvalidExpressionException("Cannot execute an invalid expression in task " + source);
                        }
                        return String.valueOf((Object) evaluator.evaluate(root));
                    }).replace(source);
        }
    }

    public String getSource() {
        return source;
    }

    @SafeVarargs
    public final <T> T getValue(ProcessTaskActor task, T... defaultValue) {
        return getValue(new InputMappingRoot(task), defaultValue);
    }

    @SafeVarargs
    public final <T> T getValue(APIRootObject<?> rootObject, T... defaultValue) {
        Object outcome = action.evaluate(rootObject);
        if (outcome == null && defaultValue.length > 0) {
            return defaultValue[0];
        } else {
            return (T) outcome;
        }
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameResolver);
    }

    protected boolean sameResolver(Resolver other) {
        return getSource().equals(other.getSource());
    }
}
