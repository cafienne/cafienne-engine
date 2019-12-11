package org.cafienne.cmmn.definition.task;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.w3c.dom.Element;

import java.time.Instant;

public class DueDateDefinition extends ConstraintDefinition {
    public DueDateDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
    }

    @Override
    public String getContextDescription() {
        String parentType = getParentElement().getType();
        String parentId = getParentElement().getId();
        // This will return something like "The required rule in HumanTask 'abc'
        return "The "+getType()+" in " + parentType + " '" + parentId + "'";
    }

    public Instant evaluate(HumanTask task) {
        return getExpressionDefinition().getEvaluator().evaluateDueDate(task, this);
    }

    /**
     * Returns the type of constraint, e.g. applicabilityRule, ifPart, repetitionRule, etc.
     *
     * @return
     */
    public String getType() {
        return "Due date expression";
    }
}