package org.cafienne.processtask.implementation.calculation;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.processtask.implementation.calculation.operation.Source;

public class Result {
    private final Calculation calculation;
    private final Source step;
    private final Value value;

    public Result(Calculation calculation, Source step, Value value) {
        this.calculation = calculation;
        this.step = step;
        this.value = value;
    }

    /**
     * Get the Result value (a clone of the internal value)
     * @param
     * @return
     */
    public Value getValue() {
        return value.cloneValueNode();
    }
}
