package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.expression.spel.api.ProcessActorRootObject;
import org.cafienne.json.Value;
import org.cafienne.processtask.instance.ProcessTaskActor;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
public class OutputMappingRoot extends ProcessActorRootObject {

    private final ParameterDefinition rawParameter;
    private final ParameterDefinition targetParameter;

    public OutputMappingRoot(ParameterDefinition rawParameter, Value<?> parameterValue, ParameterDefinition targetParameter, ProcessTaskActor processTaskActor) {
        super(processTaskActor);
        this.rawParameter = rawParameter;
        this.targetParameter = targetParameter;
        addPropertyReader(rawParameter.getName(), () -> parameterValue);
    }

    @Override
    public String getDescription() {
        return " mapping process output '" + rawParameter.getName() + "' to '" + targetParameter.getName() + "'";
    }
}
