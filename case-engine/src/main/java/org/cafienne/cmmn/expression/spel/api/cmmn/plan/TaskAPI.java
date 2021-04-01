package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.instance.Task;

/**
 */
public class TaskAPI extends PlanItemAPI<Task> {
    TaskAPI(CaseAPI caseAPI, Task task, StageAPI stage) {
        super(caseAPI, task, stage);
        addPropertyReader("input", () -> task.getMappedInputParameters());
//        addPropertyReader("output", () -> task.getOutputParameters());
    }

    @Override
    public String getName() {
        return "task";
    }
}
