package org.cafienne.cmmn.expression.spel.api.cmmn.workflow;

import org.cafienne.cmmn.definition.task.DueDateDefinition;
import org.cafienne.cmmn.expression.spel.api.cmmn.constraint.PlanItemRootAPI;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;

public class DueDateAPI extends PlanItemRootAPI {
    public DueDateAPI(DueDateDefinition constraintDefinition, HumanTask task) {
        super(constraintDefinition, task);
    }

    @Override
    public String getDescription() {
        return "due date expression";
    }
}
