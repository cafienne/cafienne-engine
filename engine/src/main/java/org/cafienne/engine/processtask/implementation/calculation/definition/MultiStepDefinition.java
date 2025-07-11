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

package org.cafienne.engine.processtask.implementation.calculation.definition;

import org.cafienne.engine.processtask.implementation.calculation.Calculation;
import org.cafienne.engine.processtask.implementation.calculation.CalculationDefinition;
import org.cafienne.engine.processtask.implementation.calculation.operation.CalculationStep;
import org.cafienne.engine.processtask.implementation.calculation.operation.MultiStep;

import java.util.List;

public class MultiStepDefinition extends StepDefinition {
    private final List<StepDefinition> steps;
    public MultiStepDefinition(CalculationDefinition calculationDefinition, String identifier, List<StepDefinition> list) {
        super(calculationDefinition, identifier);
        this.steps = list;
    }

    public List<StepDefinition> getSteps() {
        return steps;
    }

    @Override
    public CalculationStep createInstance(Calculation calculation) {
        return new MultiStep(calculation, this);
    }

    public boolean hasDependency(StepDefinition stepDefinition) {
        return this.steps.stream().anyMatch(step -> step.hasDependency(stepDefinition));
    }

    @Override
    public String getType() {
        return "Multi output step";
    }

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
