/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.definition;

import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.CalculationDefinition;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;
import org.cafienne.processtask.implementation.calculation.operation.MultiStep;

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
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
