package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.processtask.instance.ProcessTaskActor;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
public class ProcessParameterRootObject extends APIRootObject<ProcessTaskActor> {
    public ProcessParameterRootObject(String parameterName, Value<?> parameterValue, ProcessTaskActor processTaskActor) {
        super(processTaskActor);
        addPropertyReader(parameterName, () -> parameterValue);
        addPropertyReader("task", () -> processTaskActor);
    }
}
