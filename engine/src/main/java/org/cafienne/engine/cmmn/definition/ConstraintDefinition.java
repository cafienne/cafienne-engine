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

package org.cafienne.engine.cmmn.definition;

import org.cafienne.engine.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.engine.cmmn.expression.DefaultValueEvaluator;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.engine.cmmn.instance.Path;
import org.w3c.dom.Element;

public class ConstraintDefinition extends CMMNElementDefinition {
    private final ExpressionDefinition expression;
    private final String expressionType;
    private final String contextRef;
    private CaseFileItemDefinition context;
    private Path pathToContext;

    protected ConstraintDefinition(ModelDefinition definition, CMMNElementDefinition parentElement, String expressionType, boolean defaultValue) {
        super(null, definition, parentElement);
        this.expression = new ExpressionDefinition(definition, this, defaultValue);
        this.expressionType = expressionType;
        this.contextRef = "";
    }

    public ConstraintDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.expression = parse("condition", ExpressionDefinition.class, true);
        this.contextRef = parseAttribute("contextRef", false);
        this.expressionType = element.getTagName();
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        if (contextRef.isEmpty()) {
            return;
        }
        context = getCaseDefinition().findCaseFileItem(contextRef);
        if (context == null) {
            getCaseDefinition().addReferenceError(getContextDescription() + " refers to a Case File item with id '" + contextRef + "', but the corresponding Case File item cannot be found.");
        } else {
            pathToContext = context.getPath();
        }
    }

    @Override
    public String getContextDescription() {
        CMMNElementDefinition parent = getParentElement().getParentElement() != null ? getParentElement().getParentElement() : getParentElement();
        String parentType = parent.getType();
        String parentId = parent.getId();
        String parentName = parent.getName();
        // This will return something like "The required rule in HumanTask 'abc'
        return "The " + getType() + " in " + parentType + " '" + parentName + "' with id '" + parentId + "'";
    }

    /**
     * Returns the definition of the case file item context (if any) for this constraint.
     *
     * @return
     */
    public CaseFileItemDefinition getContext() {
        return context;
    }

    /**
     * Resolves the case file item context on the specified case instance. Returns null if the context is not specified.
     *
     * @param caseInstance
     * @return
     */
    public CaseFileItem resolveContext(Case caseInstance) {
        if (context == null) {
            return null;
        }
        return pathToContext.resolve(caseInstance);
    }

    public ExpressionDefinition getExpressionDefinition() {
        return expression;
    }

    public boolean evaluate(PlanItem<?> planItem) {
        return expression.getEvaluator().evaluateItemControl(planItem, this);
    }

    public boolean isDefault() {
        return expression.getEvaluator() instanceof DefaultValueEvaluator;
    }

    /**
     * Returns the type of constraint, e.g. applicabilityRule, ifPart, repetitionRule, etc.
     *
     * @return
     */
    public String getType() {
        return expressionType;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameConstraint);
    }

    public boolean sameConstraint(ConstraintDefinition other) {
        return same(expression, other.expression)
                && same(context, other.context);
    }
}
