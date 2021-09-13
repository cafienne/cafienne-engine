/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression;

import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.json.Value;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Interface to be implemented by language specific expressions in CMMN. The expression evaluator
 * will be instantiated for each expression inside the definition of the case.
 * Implementations must have a constructor that takes {@link ExpressionDefinition} as an argument
 */
public interface CMMNExpressionEvaluator {
    /**
     * The evaluate method must evaluate the expression, and can use the
     * parameters to create the context of the expression.
     *
     * @param caseInstance        The case within which the expression is to be evaluated.
     * @param contextObject       The context within the expression is to be evaluated.
     * @param ruleTypeDescription A string indicating the type of rule. Can be used to logging purposes
     * @return boolean evaluateConstraint(Case caseInstance, PlanItem planItem, String ruleTypeDescription);
     */

    /**
     * Evaluate the expression of an item control rule (Repetition, Required, ManualActivation) for a given plan item.
     * @param planItem The runtime instance context to the evaluation
     * @param ruleDefinition The rule definition context.
     * @return
     * @throws InvalidExpressionException
     */
    boolean evaluateItemControl(PlanItem<?> planItem, ConstraintDefinition ruleDefinition) throws InvalidExpressionException;

    /**
     * Evaluate an if part expression
     * @param criterion Entry or exit criterion context for the expression
     * @param ifPartDefinition Definition context for the evaluation
     * @return
     * @throws InvalidExpressionException
     */
    boolean evaluateIfPart(Criterion<?> criterion, IfPartDefinition ifPartDefinition) throws InvalidExpressionException;

    /**
     * Evaluate an applicability rule expression - whether or not the discretionary item can be planned inside the containing plan item.
     * @param containingPlanItem The instance context (Stage or HumanTask) in which the item can or cannot be planned
     * @param discretionaryItemDefinition The item definition for the item that can or cannot be planne.
     * @param ruleDefinition The actual rule definition holding the expression to be evaluated.
     * @return
     * @throws InvalidExpressionException
     */
    boolean evaluateApplicabilityRule(PlanItem<?> containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) throws InvalidExpressionException;

    /**
     * Evaluation of the expression in the context of the plan item should result in a Duration that can be used in a @{@link TimerEvent} listener.
     * @param timerEvent The context of the instance for evaluating the expression.
     * @param definition The context of the definition for the expression.
     * @return
     */
    default Duration evaluateTimerExpression(TimerEvent timerEvent, TimerEventDefinition definition) throws InvalidExpressionException {
        try {
            return Duration.parse(definition.getTimerExpression().getBody().trim());
        } catch (DateTimeParseException dtpe) {
            throw new InvalidExpressionException("The timer expression " + definition.getTimerExpression().getBody() + " in " + definition.getName() + " cannot be parsed into a Duration", dtpe);
        }
    }

    /**
     * Evaluate the mapping of an input parameter of a Task into the input parameter of a TaskImplementation.
     * @param caseInstance Case context
     * @param from Task input parameter
     * @param targetDefinition Definition of the parameter of the task implementation
     * @param task Task context
     * @return The value that goes into the new Parameter instance for the task implementation parameter
     * @throws InvalidExpressionException
     */
    Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition targetDefinition, Task<?> task) throws InvalidExpressionException;

    /**
     * Evaluate the mapping of a TaskImplementation output parameter to the Task's output parameter.
     * @param caseInstance Case context
     * @param rawOutputParameterValue Value that comes from the TaskImplementation's output parameter
     * @param rawOutputParameterDefinition Definition of the TaskImplementation's output parameter
     * @param targetParameterDefinition Definition of the Task output parameter that will be newly instantiated
     * @param task Task context
     * @return The value that will go into the new instance of the Task output parameter
     * @throws InvalidExpressionException
     */
    Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> rawOutputParameterValue, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetParameterDefinition, Task<?> task) throws InvalidExpressionException;

    default Value<?> evaluateOutputParameterTransformation(ProcessTaskActor processTaskActor, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetOutputParameterDefinition) {
        return Value.NULL;
    }

    /**
     * Evaluation of the expression in the context of the task should result in a user id that can be used in a as the assignee in a @{@link HumanTask} task.
     * @param task The context of the instance for evaluating the expression.
     * @param definition The context of the definition for the expression.
     * @return
     */
    default String evaluateAssignee(HumanTask task, AssignmentDefinition definition) throws InvalidExpressionException {
        return "";
    }

    /**
     * Evaluation of the expression in the context of the plan item should result in a due date that can be used in a @{@link HumanTask} task.
     * @param task The context of the instance for evaluating the expression.
     * @param definition The context of the definition for the expression.
     * @return
     */
    default Instant evaluateDueDate(HumanTask task, DueDateDefinition definition) throws InvalidExpressionException {
        return null;
    }
}
