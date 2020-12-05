package org.cafienne.cmmn.definition.task;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.w3c.dom.Element;

public class AssignmentDefinition extends ConstraintDefinition {
    public AssignmentDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public String getContextDescription() {
        String parentType = getParentElement().getType();
        String parentId = getParentElement().getId();
        // This will return something like "The required rule in HumanTask 'abc'
        return "The "+getType()+" in " + parentType + " '" + parentId + "'";
    }

    public String evaluate(HumanTask task) {
        return getExpressionDefinition().getEvaluator().evaluateAssignee(task, this);
    }

    /**
     * Returns the type of constraint, e.g. applicabilityRule, ifPart, repetitionRule, etc.
     *
     * @return
     */
    public String getType() {
        return "assignment";
    }
}