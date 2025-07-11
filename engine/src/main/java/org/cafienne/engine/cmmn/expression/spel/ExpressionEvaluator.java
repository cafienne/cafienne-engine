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

package org.cafienne.engine.cmmn.expression.spel;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.cafienne.engine.cmmn.definition.ApplicabilityRuleDefinition;
import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ConstraintDefinition;
import org.cafienne.engine.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.engine.cmmn.definition.ExpressionDefinition;
import org.cafienne.engine.cmmn.definition.TimerEventDefinition;
import org.cafienne.engine.cmmn.definition.extension.workflow.AssignmentDefinition;
import org.cafienne.engine.cmmn.definition.extension.workflow.DueDateDefinition;
import org.cafienne.engine.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.engine.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.engine.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.engine.cmmn.expression.InvalidExpressionException;
import org.cafienne.engine.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.constraint.ApplicabilityRuleAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.constraint.IfPartAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.constraint.PlanItemRootAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.mapping.TaskInputMappingAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.mapping.TaskOutputMappingAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.plan.TimerExpressionAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.workflow.AssignmentAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.workflow.DueDateAPI;
import org.cafienne.engine.cmmn.expression.spel.api.process.CalculationAPI;
import org.cafienne.engine.cmmn.expression.spel.api.process.OutputMappingRoot;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.engine.cmmn.instance.Task;
import org.cafienne.engine.cmmn.instance.TimerEvent;
import org.cafienne.engine.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.engine.cmmn.instance.sentry.Criterion;
import org.cafienne.engine.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.engine.processtask.implementation.calculation.Calculation;
import org.cafienne.engine.processtask.implementation.calculation.definition.expression.CalculationExpressionDefinition;
import org.cafienne.engine.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.engine.processtask.implementation.calculation.operation.CalculationStep;
import org.cafienne.engine.processtask.instance.ProcessTaskActor;
import org.cafienne.json.LongValue;
import org.cafienne.json.StringValue;
import org.cafienne.json.Value;

public class ExpressionEvaluator implements CMMNExpressionEvaluator {
    private final Evaluator evaluator;
    private final String expressionString;

    public ExpressionEvaluator(ExpressionDefinition expressionDefinition) {
        this(expressionDefinition, expressionDefinition.getBody());
    }

    public ExpressionEvaluator(CalculationExpressionDefinition expressionDefinition) {
        this(expressionDefinition, expressionDefinition.getExpression());
    }

    private ExpressionEvaluator(CMMNElementDefinition expressionDefinition, String expressionString) {
        this.evaluator = new Evaluator(expressionDefinition, expressionString);
        this.expressionString = expressionString;
    }

    /**
     * @param <T>
     * @param rootObject
     * @return
     */
    private <T> T evaluateExpression(APIRootObject<?> rootObject) {
        return evaluator.evaluate(rootObject);
    }

    private boolean evaluateConstraint(PlanItemRootAPI<?> contextObject) {
        Object outcome = evaluateExpression(contextObject);
        if (outcome instanceof Boolean) {
            return (boolean) outcome;
        } else {
            contextObject.getActor().addDebugInfo(() -> "Failure in evaluating " + contextObject.getDescription() + ", with expression "
                    + expressionString.trim()
                    + "\nIt should return something of type boolean instead of type " + outcome.getClass().getName()
                    + "\nReturning result 'false'");
            return false;
        }
    }

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        return Value.convert(evaluateExpression(new TaskInputMappingAPI(from, to, task)));
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition, Task<?> task) {
        return Value.convert(evaluateExpression(new TaskOutputMappingAPI(rawOutputParameterDefinition, targetOutputParameterDefinition, value, task)));
//        return evaluateParameterTransformation(rawOutputParameterDefinition.getName(), value, task);
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(ProcessTaskActor processTaskActor, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition) {
        OutputMappingRoot contextObject = new OutputMappingRoot(rawOutputParameterDefinition, value, targetOutputParameterDefinition, processTaskActor);
        Object result = evaluateExpression(contextObject);
        return Value.convert(result);
    }

    @Override
    public Duration evaluateTimerExpression(TimerEvent timerEvent, TimerEventDefinition definition) {
        try {
            return Duration.parse(definition.getTimerExpression().getBody().trim());
        } catch (DateTimeParseException dtpe) {
            // Failed to do default parsing. Let's try to put a SPEL onto it....
        }
        // If the result is an actual Duration instance we are done. Otherwise we will try to parse the result as Duration
        Object result = evaluateExpression(new TimerExpressionAPI(timerEvent));
        if (result instanceof Duration) {
            return (Duration) result;
        }
        // Apparently something else than a Duration, so we will just try to parse the string of it
        try {
            return Duration.parse(String.valueOf(result).trim());
        } catch (DateTimeParseException dtpe) {
            throw new InvalidExpressionException("The timer expression " + definition.getTimerExpression().getBody() + " in " + definition.getName() + " cannot be parsed into a Duration", dtpe);
        }
    }

    @Override
    public boolean evaluateItemControl(PlanItem<?> planItem, ConstraintDefinition ruleDefinition) {
        return evaluateConstraint(new PlanItemRootAPI<>(ruleDefinition, planItem));
    }

    @Override
    public boolean evaluateIfPart(Criterion<?> criterion, IfPartDefinition ifPartDefinition) {
        return evaluateConstraint(new IfPartAPI(ifPartDefinition, criterion));
    }

    @Override
    public boolean evaluateApplicabilityRule(PlanItem<?> containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        return evaluateConstraint(new ApplicabilityRuleAPI(containingPlanItem, discretionaryItemDefinition, ruleDefinition));
    }

    @Override
    public Instant evaluateDueDate(HumanTask task, DueDateDefinition definition) throws InvalidExpressionException {
        Object outcome = evaluateExpression(new DueDateAPI(definition, task));
        if (outcome == null || outcome == Value.NULL) {
            return null;
        }
        if (outcome instanceof Instant) {
            return (Instant) outcome;
        }
        if (outcome instanceof java.util.Date) {
            return ((java.util.Date) outcome).toInstant();
        }
        if (outcome instanceof Long) {
            return Instant.ofEpochMilli((Long) outcome);
        }
        if (outcome instanceof LongValue) {
            return Instant.ofEpochMilli(((LongValue) outcome).getValue());
        }
        if (outcome instanceof String) {
            try {
                return Instant.parse(outcome.toString());
            } catch (DateTimeParseException e) {
                throw new InvalidExpressionException("Cannot parse string '" + outcome + "' to Instant: " + e.getMessage(), e);
            }
        }
        if (outcome instanceof StringValue) {
            try {
                return Instant.parse(((StringValue) outcome).getValue());
            } catch (DateTimeParseException e) {
                throw new InvalidExpressionException("Cannot parse string '" + outcome + "' to Instant: " + e.getMessage(), e);
            }
        }
        throw new InvalidExpressionException("Outcome of due date expression cannot be interpreted as a due date, it is of type " + outcome.getClass().getName());
    }

    @Override
    public String evaluateAssignee(HumanTask task, AssignmentDefinition definition) throws InvalidExpressionException {
        Object outcome = evaluateExpression(new AssignmentAPI(definition, task));
        if (outcome == null || outcome == Value.NULL) {
            return null;
        }
        return outcome.toString();
    }

    /**
     * Evaluate a single step in a calculation task.
     * @param calculation
     * @param step
     * @param sources
     * @return
     * @throws InvalidExpressionException
     */
    public Value<?> runCalculationStep(Calculation calculation, CalculationStep step, Map<InputReference, Value<?>> sources) throws InvalidExpressionException {
        CalculationAPI context = new CalculationAPI(calculation, step, sources);
        Object result = evaluateExpression(context);
        return Value.convert(result);
    }
}
