/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.operation;

import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.MultiStepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;

import java.util.List;

public class MultiStep extends CalculationStep {
    private final MultiStepDefinition definition;
    private CalculationStep actualStep;

    public MultiStep(Calculation calculation, MultiStepDefinition definition) {
        super(calculation, definition);
        this.definition = definition;
    }

    public boolean isValid() {
        List<StepDefinition> options = definition.getSteps();
        addDebugInfo(() -> "Checking isValid for " + definition.getIdentifier() +" on " + options.size() +" possible steps");
        for (StepDefinition stepDefinition : options) {
            actualStep = stepDefinition.createInstance(calculation);
            if (actualStep.isValid()) {
                addDebugInfo(() -> "actualStep["+definition.getIdentifier()+"]." + options.indexOf(stepDefinition) +".isValid() = " + true);
                return true;
            } else {
                addDebugInfo(() -> "actualStep["+definition.getIdentifier()+"]." + options.indexOf(stepDefinition) +".isValid() = " + false);
            }
        }
        addDebugInfo(() -> "Could not find a valid actual step, returning false on " + definition.getIdentifier());
        return false;
    }

    protected Result calculateResult() {
        return actualStep.calculateResult();
    }
}
