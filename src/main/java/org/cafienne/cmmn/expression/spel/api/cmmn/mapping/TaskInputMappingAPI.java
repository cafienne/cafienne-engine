package org.cafienne.cmmn.expression.spel.api.cmmn.mapping;

import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
public class TaskInputMappingAPI extends TaskMappingAPI {
    private final TaskInputParameter from;
    private final ParameterDefinition to;

    public TaskInputMappingAPI(TaskInputParameter from, ParameterDefinition to, Task<?> task) {
        super(from.getDefinition().getName(), from.getValue(), task);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getDescription() {
        return "mapping task input parameter '" + from.getDefinition().getName() + "' to '" + to.getName() + "'";
    }
}
