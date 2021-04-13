package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.cmmn.expression.spel.api.ProcessActorRootObject;
import org.cafienne.cmmn.expression.spel.api.cmmn.constraint.PlanItemRootAPI;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.SourceDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;

import java.util.stream.Collectors;

/**
 * Provides context for steps in calculation tasks
 */
public class CalculationAPI extends CaseRootObject {
    private final Calculation calculation;
    private final CalculationStep step;

    public CalculationAPI(Calculation calculation, CalculationStep step, ValueMap sources) {
        super(calculation.getTask().getCaseInstance());
        this.calculation = calculation;
        this.step = step;

        // Make sure we can directly access the task or stage or milestone; e.g. "task.index < 3"
        registerPlanItem(calculation.getTask());

        // Add a reader for each incoming source
        sources.fieldNames().forEachRemaining(field -> {
            final Value value = sources.get(field);
            addPropertyReader(field, () -> value);
        });
    }

    @Override
    public String getDescription() {
        String from = step.getDefinition().getSources().stream().map(SourceDefinition::getIdentifier).collect(Collectors.joining("', '"));
        String fromSourcesString = from.isEmpty() ? "" : " from '" + from + "'";
        return "calculation step to produce '" + step.getDefinition().getIdentifier() + "'" + fromSourcesString + ". Expression";
    }
}
