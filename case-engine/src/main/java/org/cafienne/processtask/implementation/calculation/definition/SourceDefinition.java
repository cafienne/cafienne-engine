package org.cafienne.processtask.implementation.calculation.definition;

import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.operation.Source;

import java.util.Collection;

public interface SourceDefinition {
    /**
     * Determine whether the source is dependent on the mapping
     *
     * @param stepDefinition
     * @return
     */
    boolean hasDependency(StepDefinition stepDefinition);

    Source createInstance(Calculation calculation);

    String getIdentifier();

    default String getType() {
        return this.getClass().getSimpleName();
    }

    Collection<SourceDefinition> getSources();

    default String getDescription() {
        return this.getType() + " '" + getIdentifier() + "'";
    }
}
