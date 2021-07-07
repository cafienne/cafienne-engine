package org.cafienne.processtask.implementation.calculation.definition.source;

import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.Source;

import java.util.Collection;
import java.util.Collections;

public interface SourceDefinition {
    /**
     * Determine whether the source is dependent on the mapping
     *
     * @param stepDefinition
     * @return
     */
    boolean hasDependency(StepDefinition stepDefinition);

    Source<?> createInstance(Calculation calculation);

    String getIdentifier();

    default String getType() {
        return this.getClass().getSimpleName();
    }

    default String getDescription() {
        return this.getType() + " '" + getIdentifier() + "'";
    }

    /**
     * Return sources that this source is dependent on.
     * @return
     */
    default Collection<InputReference> getInputs() {
        return Collections.emptySet();
    }
}
