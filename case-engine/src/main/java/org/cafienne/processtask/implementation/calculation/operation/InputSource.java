package org.cafienne.processtask.implementation.calculation.operation;

import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.source.InputParameterSourceDefinition;

public class InputSource extends Source<InputParameterSourceDefinition> {
    private final InputParameterDefinition parameter;

    public InputSource(InputParameterSourceDefinition inputParameterSourceDefinition, Calculation calculation, InputParameterDefinition parameter) {
        super(inputParameterSourceDefinition, calculation);
        this.parameter = parameter;
    }

    /**
     * Return the outcome of the calculation, if required first calculate it.
     *
     * @return
     */
    protected Result calculateResult() {
        return new Result(calculation, this, calculation.getTask().getMappedInputParameters().get(parameter.getName()));
    }
}
