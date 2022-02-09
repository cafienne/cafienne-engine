package org.cafienne.cmmn.expression.spel.api.cmmn.workflow;

import org.cafienne.cmmn.definition.task.AssignmentDefinition;
import org.cafienne.cmmn.expression.spel.api.cmmn.constraint.PlanItemRootAPI;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;

public class AssignmentAPI extends PlanItemRootAPI {
    public AssignmentAPI(AssignmentDefinition constraintDefinition, HumanTask task) {
        super(constraintDefinition, task);
    }

    @Override
    public String getDescription() {
        return "assignment expression";
    }
}
