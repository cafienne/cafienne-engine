/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.expression.DefaultValueEvaluator;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.Path;
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
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameConstraint);
    }

    public boolean sameConstraint(ConstraintDefinition other) {
        return same(expression, other.expression)
                && same(context, other.context);
    }
}
