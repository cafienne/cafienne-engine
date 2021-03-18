/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.serialization.json.LongValue;
import org.cafienne.akka.actor.serialization.json.StringValue;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.expression.CMMNExpressionEvaluator;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.cmmn.expression.spel.api.cmmn.constraint.ApplicabilityRuleAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.constraint.IfPartAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.constraint.PlanItemRootAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.plan.TaskParameterAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.plan.TimerExpressionAPI;
import org.cafienne.cmmn.expression.spel.api.process.ProcessParameterRootObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class ExpressionEvaluator implements CMMNExpressionEvaluator {
    private final ExpressionParser parser;
    private final Expression spelExpression;
    private final String expressionString;
    private final ExpressionDefinition expressionDefinition;

    public ExpressionEvaluator(ExpressionDefinition expressionDefinition) {
        this.parser = new SpelExpressionParser();
        this.expressionString = expressionDefinition.getBody();
        this.expressionDefinition = expressionDefinition;
        this.spelExpression = parseExpression();
    }

    private Expression parseExpression() {
        try {
            return parser.parseExpression(expressionString);
        } catch (SpelParseException spe) {
            expressionDefinition.getModelDefinition().addDefinitionError(expressionDefinition.getContextDescription() + " has an invalid expression:\n" + spe.getMessage());
            return null;
        }
    }

    /**
     * @param <T>
     * @param rootObject
     * @param contextDescription
     * @return
     */
    private <T> T evaluateExpression(APIRootObject rootObject, String contextDescription) {
        ModelActor actor = rootObject.getActor();
        // System.out.println("Now evaluating the expression " + definition.getBody());
        StandardEvaluationContext context = new StandardEvaluationContext(rootObject);
        // The case file accessor can be used to dynamically resolve properties that belong to the case file
        SpelReadableRecognizer spelPropertyReader = new SpelReadableRecognizer(actor);
        context.addPropertyAccessor(spelPropertyReader);

        // TODO: improve the type checking and raise better error message if we're getting back the wrong type.

        try {
            actor.addDebugInfo(() -> "Evaluating " + contextDescription + ": " + expressionString.trim());
            // Not checking it. If it fails, it really fails.
            @SuppressWarnings("unchecked")
            T value = (T) spelExpression.getValue(context);
            actor.addDebugInfo(() -> "Outcome: " + value);
            return value;
        } catch (EvaluationException invalidExpression) {
            actor.addDebugInfo(() -> "Failure in evaluating " + contextDescription + ", with expression " + expressionString.trim(), invalidExpression);
            throw new InvalidExpressionException("Could not evaluate " + spelExpression.getExpressionString() + "\n" + invalidExpression.getLocalizedMessage(), invalidExpression);
        }
    }

    private boolean evaluateConstraint(PlanItemRootAPI contextObject, String constraintDescription) {
        Object outcome = evaluateExpression(contextObject, constraintDescription);
        if (outcome instanceof Boolean) {
            return (boolean) outcome;
        } else {
            contextObject.getActor().addDebugInfo(() -> "Failure in evaluating " + constraintDescription + ", with expression "
                    + expressionString.trim()
                    + "\nIt should return something of type boolean instead of type " + outcome.getClass().getName()
                    + "\nReturning result 'false'");
            return false;
        }
    }

    private Value<?> evaluateParameterTransformation(Case caseInstance, String parameterName, Value<?> parameterValue, Task<?> task) {
        TaskParameterAPI contextObject = new TaskParameterAPI(parameterName, parameterValue, task);
        Object result = evaluateExpression(contextObject, "parameter transformation");
        return Value.convert(result);
    }

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        return evaluateParameterTransformation(caseInstance, from.getDefinition().getName(), from.getValue(), task);
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition, Task<?> task) {
        return evaluateParameterTransformation(caseInstance, rawOutputParameterDefinition.getName(), value, task);
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(ProcessTaskActor processTaskActor, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition) {
        ProcessParameterRootObject contextObject = new ProcessParameterRootObject(rawOutputParameterDefinition.getName(), value, processTaskActor);
        Object result = evaluateExpression(contextObject, "parameter transformation");
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
        Object result = evaluateExpression(new TimerExpressionAPI(timerEvent), "timer event duration");
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
    public boolean evaluateItemControl(PlanItem planItem, ConstraintDefinition ruleDefinition) {
        return evaluateConstraint(new PlanItemRootAPI(ruleDefinition, planItem), ruleDefinition.getContextDescription());
    }

    @Override
    public boolean evaluateIfPart(Criterion criterion, IfPartDefinition ifPartDefinition) {
        return evaluateConstraint(new IfPartAPI(ifPartDefinition, criterion), "ifPart in sentry");
    }

    @Override
    public boolean evaluateApplicabilityRule(PlanItem containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        String description = "applicability rule '" + ruleDefinition.getName() + "' for discretionary item " + discretionaryItemDefinition;
        return evaluateConstraint(new ApplicabilityRuleAPI(containingPlanItem, discretionaryItemDefinition, ruleDefinition), description);
    }

    @Override
    public Instant evaluateDueDate(HumanTask task, DueDateDefinition definition) throws InvalidExpressionException {
        Object outcome = evaluateExpression(new PlanItemRootAPI(definition, task), "due date expression");
        if (outcome == null || outcome == Value.NULL) {
            return null;
        }
        if (outcome instanceof Instant) {
            return (Instant) outcome;
        }
        if (outcome instanceof java.util.Date) {
            return ((java.util.Date) outcome).toInstant();
        }
        if (outcome instanceof java.sql.Date) {
            return ((java.sql.Date) outcome).toInstant();
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
        Object outcome = evaluateExpression(new PlanItemRootAPI(definition, task), "assignment expression");
        if (outcome == null || outcome == Value.NULL) {
            return null;
        }
        return outcome.toString();
    }
}
