package org.cafienne.cmmn.expression.spel.api.cmmn.mapping;

import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.json.Value;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
public class TaskOutputMappingAPI extends TaskMappingAPI {
    private final ParameterDefinition from;
    private final ParameterDefinition to;

    public TaskOutputMappingAPI(ParameterDefinition from, ParameterDefinition to, Value<?> value, Task<?> task) {
        super(from.getName(), value, task);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getDescription() {
        return "mapping raw output parameter '" + from.getName() + "' to task output parameter '" + to.getName() + "'";
    }
}
