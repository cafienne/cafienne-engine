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

package com.casefabric.cmmn.expression.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.casefabric.actormodel.ModelActor;
import com.casefabric.cmmn.definition.ApplicabilityRuleDefinition;
import com.casefabric.cmmn.definition.ConstraintDefinition;
import com.casefabric.cmmn.definition.DiscretionaryItemDefinition;
import com.casefabric.cmmn.definition.ExpressionDefinition;
import com.casefabric.cmmn.definition.parameter.ParameterDefinition;
import com.casefabric.cmmn.definition.sentry.IfPartDefinition;
import com.casefabric.cmmn.expression.CMMNExpressionEvaluator;
import com.casefabric.cmmn.expression.InvalidExpressionException;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.PlanItem;
import com.casefabric.cmmn.instance.Task;
import com.casefabric.cmmn.instance.parameter.TaskInputParameter;
import com.casefabric.cmmn.instance.sentry.Criterion;
import com.casefabric.json.Value;
import com.casefabric.processtask.instance.ProcessTaskActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpressionEvaluator implements CMMNExpressionEvaluator {
    private final static Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);
    private final String jsonPath;
    private final ExpressionDefinition definition;

    public ExpressionEvaluator(ExpressionDefinition expressionDefinition) {
        jsonPath = expressionDefinition.getBody();
        definition = expressionDefinition;
    }

    public boolean evaluateConstraint(Case caseInstance, Object contextObject, String ruleTypeDescription) {
        caseInstance.addDebugInfo(() -> "Now evaluating the expression " + jsonPath);
        String json = String.valueOf(contextObject);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);

        boolean value = Boolean.parseBoolean(JsonPath.read(document, jsonPath));

        return value;
    }

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        return evaluateJSON(caseInstance, from.getValue());
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition, Task<?> task) {
        return evaluateJSON(caseInstance, value);
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(ProcessTaskActor processTaskActor, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition) {
        return evaluateJSON(processTaskActor, value);
    }

    private Value<?> evaluateJSON(ModelActor caseInstance, Value<?> value) {
        // First check if there is something at all to evaluate on. If not, return immediately.
        if (value == null || value.equals(Value.NULL)) {
            // Just can't read from null
            caseInstance.addDebugInfo(() -> "Skipping the json path evaluation of expression "+jsonPath+", because input value is null; returning Value.NULL");
            return Value.NULL;
        }

        // Announce we're doing this
        caseInstance.addDebugInfo(() -> "Evaluating expression " + jsonPath +" on ", value);

        // Convert the Value<?> to String, because there is no ValueMap implementation for JsonPath (yet)
        String json = String.valueOf(value);

        // Also check if the value is simply empty (can typically happen when a StringValue object was created with an empty string
        if (json.trim().isEmpty()) {
            // Just can't read from an empty string
            caseInstance.addDebugInfo(() -> "Skipping the json path evaluation of expression "+jsonPath+", because input value is empty; returning Value.NULL");
            return Value.NULL;
        }

        try {
            Object result = JsonPath.read(json, jsonPath);
            Value<?> output = Value.convert(result); // Typically a ValueMap or a ValueList
            // JsonPath returns single element results sometimes in an array; then we'll return that value instead.
            if (output.isList()) {
                if (output.asList().size() == 1) {
                    output = output.asList().get(0);
                    String outputClassName = output.getClass().getSimpleName();
                    caseInstance.addDebugInfo(() -> "Resulting array structure has only one element; returning element instead of array. Element has type " + outputClassName);
                }
            }

            final Value<?> finalOutput = output; // So that we can use it in the logging lambda

            caseInstance.addDebugInfo(() -> "Result of json evaluation: ", finalOutput);
            return finalOutput;

        } catch (InvalidJsonException e) {
            // Note: this is never supposed to happen, since the input is a Value<?>, which cannot be anything but valid and parseable JSON
            throw new InvalidExpressionException("Cannot evaluate json path", e.fillInStackTrace());
        } catch (JsonPathException jpe) {
            String msg = "The expression could not be resolved on the object due to a path exception - " + jpe.getMessage();
            caseInstance.addDebugInfo(() -> msg);
            logger.warn(msg);
            return Value.NULL;
        }
    }

    @Override
    public boolean evaluateItemControl(PlanItem<?> planItem, ConstraintDefinition ruleDefinition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean evaluateIfPart(Criterion<?> criterion, IfPartDefinition ifPartDefinition) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean evaluateApplicabilityRule(PlanItem<?> containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        // TODO Auto-generated method stub
        return false;
    }
}
