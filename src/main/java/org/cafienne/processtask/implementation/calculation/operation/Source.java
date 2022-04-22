package org.cafienne.processtask.implementation.calculation.operation;

import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;

public abstract class Source<D extends SourceDefinition> {
    private Result result;
    protected final D definition;
    protected final Calculation calculation;

    protected Source(D definition, Calculation calculation) {
        this.definition = definition;
        this.calculation = calculation;
    }

    protected void addDebugInfo(DebugInfoAppender appender) {
        calculation.getTask().getCaseInstance().addDebugInfo(appender);
    }

    public D getDefinition() {
        return definition;
    }

    /**
     * Return the outcome of the calculation step, if required first calculate it.
     * @return
     */
    public final Result getResult() {
        if (this.result == null) {
            this.result = calculateResult();
        }
        return this.result;
    }

    /**
     * Return the outcome of the calculation step.
     * This will be invoked only once, from the getResult method.
     * @return
     */
    protected abstract Result calculateResult();

    public boolean isValid() {
        return true;
    }
}
