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

package org.cafienne.cmmn.expression;

import org.cafienne.cmmn.definition.ApplicabilityRuleDefinition;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.json.Value;

/**
 * Simplest implementation of the CMMNExpressionEvaluator. Intended to return default values
 * for expressions such as repetition rule, etc. Simply returns the default value upon evaluation.
 *
 */
public class DefaultValueEvaluator implements CMMNExpressionEvaluator {
    private final boolean defaultValue;

    public DefaultValueEvaluator(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Value<?> evaluateInputParameterTransformation(Case caseInstance, TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        return from.getValue();
    }

    @Override
    public Value<?> evaluateOutputParameterTransformation(Case caseInstance, Value<?> value, ParameterDefinition rawOutputParameterDefinition, ParameterDefinition targetParameterDefinition, Task<?> task) {
        return value;
    }

    @Override
    public boolean evaluateItemControl(PlanItem<?> planItem, ConstraintDefinition ruleDefinition) {
        return defaultValue;
    }

    @Override
    public boolean evaluateIfPart(Criterion<?> criterion, IfPartDefinition ifPartDefinition) {
        return defaultValue;
    }

    @Override
    public boolean evaluateApplicabilityRule(PlanItem<?> containingPlanItem, DiscretionaryItemDefinition discretionaryItemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        return defaultValue;
    }

}
