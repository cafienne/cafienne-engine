package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.akka.actor.serialization.json.Value;

/**
 * Provides context for input/output transformation of parameters.
 * Can read the parameter name in the expression and resolve it to the parameter value.
 * Contains furthermore a task property, to provide for the task context for which this parameter transformation is being executed.
 */
class ParameterContext extends ExpressionContext {
    public final Object task;
    private final String parameterName;
    private final Value<?> parameterValue;

    protected ParameterContext(String parameterName, Value<?> parameterValue, ModelActor caseInstance, Task<?> task) {
        super(caseInstance);
        if (caseInstance instanceof Case) {
            this.task = task;
        } else {
            this.task = caseInstance;
        }
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    @Override
    public Value<?> read(String propertyName) {
        if (propertyName.equals(parameterName)) {
            return parameterValue;
        }
        return null;
    }

    @Override
    public boolean canRead(String propertyName) {
        return propertyName.equals(parameterName);
    }
}
