package org.cafienne.cmmn.expression.spel.api.cmmn.mapping;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.cmmn.instance.Task;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
abstract class TaskMappingAPI extends CaseRootObject {
    protected TaskMappingAPI(String parameterName, Value<?> parameterValue, Task<?> task) {
        super(task.getCaseInstance());
        addPropertyReader(parameterName, () -> parameterValue);
        registerPlanItem(task);
    }
}
