/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
