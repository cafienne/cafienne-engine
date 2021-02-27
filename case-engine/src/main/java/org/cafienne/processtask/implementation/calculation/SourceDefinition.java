package org.cafienne.processtask.implementation.calculation;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;

abstract class SourceDefinition {
    /**
     * Get the source's value
     * @param calculation
     * @return
     */
    abstract Value getValue(Calculation calculation);

    /**
     * Determine whether the source is dependent on the mapping
     * @param mappingDefinition
     * @return
     */
    abstract boolean hasDependency(MappingDefinition mappingDefinition);
}

class MappingSourceDefinition extends SourceDefinition {
    private final MappingDefinition mapping;

    MappingSourceDefinition(MappingDefinition mapping) {
        this.mapping = mapping;
    }

    Value getValue(Calculation calculation) {
        return mapping.getValue(calculation);
    }

    @Override
    boolean hasDependency(MappingDefinition mappingDefinition) {
        return this.mapping.hasDependency(mappingDefinition);
    }
}

class InputParameterSourceDefinition extends SourceDefinition {
    private final InputParameterDefinition parameter;

    InputParameterSourceDefinition(InputParameterDefinition parameter) {
        this.parameter = parameter;
    }

    Value getValue(Calculation calculation) {
        return calculation.getTask().getMappedInputParameters().get(parameter.getName());
    }

    @Override
    boolean hasDependency(MappingDefinition mappingDefinition) {
        return false;
    }
}
