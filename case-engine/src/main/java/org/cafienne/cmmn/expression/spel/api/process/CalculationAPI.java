package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.json.Value;
import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides context for steps in calculation tasks
 */
public class CalculationAPI extends CaseRootObject {
    private final CalculationStep step;

    public CalculationAPI(Calculation calculation, CalculationStep step, Map<InputReference, Value> sources) {
        super(calculation.getTask().getCaseInstance());
        this.step = step;

        // Make sure we can directly access the task or stage or milestone; e.g. "task.index < 3"
        registerPlanItem(calculation.getTask());

        // Add a reader for each incoming source
        sources.forEach((input, value) -> addPropertyReader(input.getElementName(), () -> value));
    }

    @Override
    public String getDescription() {
        String from = step.getDefinition().getInputs().stream().map(InputReference::getSourceReference).collect(Collectors.joining("', '"));
        String fromSourcesString = from.isEmpty() ? "" : " from '" + from + "'";
        return "calculation step to produce '" + step.getDefinition().getIdentifier() + "'" + fromSourcesString + ". Expression";
    }
}
