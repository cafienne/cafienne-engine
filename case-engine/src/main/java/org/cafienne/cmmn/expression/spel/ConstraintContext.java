/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.expression.spel;

import org.cafienne.cmmn.definition.ApplicabilityRuleDefinition;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.CaseFileItemArray;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.sentry.Sentry;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;

/**
 * A ConstraintContext provides named context to expressions that are executed
 * from within Constraints (see {@link ApplicabilityRuleContext}, {@link IfPartContext} and {@link ItemControlContext}).
 * Constraints can take a CaseFileItem as context, and the name of this CaseFileItem can be used in the expression.
 * <p></p>
 * Furthermore we have the following properties available:
 * <ul>
 *     <li><code>[case file item name]</code> - reference to the case file item content; E.g. if the context of an IfPart is a Case File Item named <code>Customer</code>, having a property <code>"name"</code> then the
 *     if part expression can contain something like <code>Customer.name == "Smith"</code></li>
 *     <li><code>caseFileItem</code> - reference to the {@link CaseFileItem} holding the context to the actual value. An example of caseFileItem usage can be an <code>Order</code> with multiple <code>Line</code> items in it. E.g. a sentry that only activates
 *     if there are more than 3 Lines available could look like <code>caseFileItem.current().index &gt; 2</code>. <p></p>
 *     Note that in case of a cardinality * the expression <code>caseFileItem</code> refers to the {@link CaseFileItemArray}, and NOT to the specific instance that is mentioned in the previous bullet.<br/>
 *     This enables expressions like <code>caseFileItem.size()</code> to find out the number of case file items in the array. <br/>
 *     Note further that using <code>[case file item name]</code> in an expression is equivalent to <code>caseFileItem.current.value</code>
 *     </li>
 *     <li><code>definition</code> - reference to the {@link ConstraintDefinition} holding the expression (e.g. a repetition rule or a required rule)</li>
 *     <li><code>caseInstance</code> - direct reference to the case instance, see {@link ExpressionContext} for examples</li>
 * </ul>
 *
 * <p>
 * See also {@link ExpressionContext}
 * @param <T> A generic referencing the definition of the constraint; can be accessed in the expression through <code>definition</code>
 */
class ConstraintContext<T extends ConstraintDefinition> extends ExpressionContext {
    public final CaseFileItem caseFileItem;
    private final String contextName;
    public final T definition;

    protected ConstraintContext(T constraint, Case caseInstance) {
        super(caseInstance);
        this.caseFileItem = constraint.resolveContext(caseInstance);
        this.contextName = constraint.getContext() != null ? constraint.getContext().getName() : null;
        this.definition = constraint;
    }

    @Override
    public Value<?> read(String propertyName) {
        if (propertyName.equals(contextName)) {
            Value<?> value = caseFileItem.getCurrent().getValue();
            return value;
        } else { // How on earth did we end up here???
            return null;
        }
    }

    @Override
    public boolean canRead(String propertyName) {
        return propertyName.equals(contextName);
    }
}

/**
 * ItemControl is defined for Required rule, Repetition rule and Manual Activation rule.
 * These rules always execute on a PlanItem, hence the plan item is also provided as an
 * additional context, through <code>planItem</code>.
 */
class ItemControlContext extends ConstraintContext<ConstraintDefinition> {
    /**
     * The plan item on which this rule is executed
     */
    public final PlanItem planItem;

    protected ItemControlContext(ConstraintDefinition constraint, PlanItem planItem) {
        super(constraint, planItem.getCaseInstance());
        this.planItem = planItem;
    }
}

/**
 * Applicability rules are executed on discretionary items related to a Stage or HumanTask.
 * This context provides the additional information with the properties <code>planItem</code> and <code>discretionaryItem</code>.
 * <p>See {@link ApplicabilityRuleContext#planItem} and {@link ApplicabilityRuleContext#discretionaryItem}
 */
class ApplicabilityRuleContext extends ConstraintContext<ApplicabilityRuleDefinition> {
    /**
     * The plan item (either Stage or HumanTask) on which the discretionary item is defined
     */
    public final PlanItem planItem;
    /**
     * The definition of the discretionary item (can be used e.g. to fetch the name)
     */
    public final DiscretionaryItemDefinition discretionaryItem;

    protected ApplicabilityRuleContext(PlanItem planItem, DiscretionaryItemDefinition itemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        super(ruleDefinition, planItem.getCaseInstance());
        this.planItem = planItem;
        this.discretionaryItem = itemDefinition;
    }

    @Override
    public Value<?> read(String propertyName) {
        if (propertyName.equals("planItem"))
            return Value.convert(planItem);
        if (propertyName.equals("discretionaryItem"))
            return Value.convert(discretionaryItem);
        return super.read(propertyName);
    }
}

/**
 * Context for evaluation of an if part in a sentry.
 */
class IfPartContext extends ConstraintContext<IfPartDefinition> {
    /**
     * The sentry for which the if part is being evaluated.
     */
    public final Sentry sentry;

    protected IfPartContext(IfPartDefinition ifPartDefinition, Sentry sentry) {
        super(ifPartDefinition, sentry.getCaseInstance());
        this.sentry = sentry;
    }

    @Override
    public Value<?> read(String propertyName) {
        if (propertyName.equals("sentry"))
            return Value.convert(sentry);
        return super.read(propertyName);
    }
}

class DueDateContext extends ConstraintContext<DueDateDefinition> {
    public final HumanTask task;

    protected DueDateContext(DueDateDefinition definition, HumanTask task) {
        super(definition, task.getCaseInstance());
        this.task = task;
    }

    @Override
    public Value<?> read(String propertyName) {
        if (propertyName.equals("task"))
            return Value.convert(task);
        return super.read(propertyName);
    }
}

class AssignmentContext extends ConstraintContext<AssignmentDefinition> {
    public final HumanTask task;

    protected AssignmentContext(AssignmentDefinition definition, HumanTask task) {
        super(definition, task.getCaseInstance());
        this.task = task;
    }

    @Override
    public Value<?> read(String propertyName) {
        if (propertyName.equals("task"))
            return Value.convert(task);
        return super.read(propertyName);
    }
}