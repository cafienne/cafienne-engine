package org.cafienne.cmmn.expression.spel;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.instance.ProcessTaskActor;

/**
 * Provides context for steps in calculation tasks
 */
class CalculationContext extends ExpressionContext {
    public CalculationContext(Calculation calculation, ValueMap sources) {
        super(calculation.getTask());
        // Add a reader for each incoming source
        sources.fieldNames().forEachRemaining(field -> {
            final Value value = sources.get(field);
            addPropertyReader(field, () -> value);
        });
        // And one for the task
        addPropertyReader("task", () -> calculation.getTask());
    }
}
