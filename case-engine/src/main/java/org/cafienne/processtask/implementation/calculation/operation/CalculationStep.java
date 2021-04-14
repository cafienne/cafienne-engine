/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation.operation;

import org.cafienne.akka.actor.serialization.json.BooleanValue;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.expression.ConditionDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;

public class CalculationStep extends Source<StepDefinition> {
    private ValueMap inputs = null;
    private final ConditionDefinition condition;

    public CalculationStep(Calculation calculation, StepDefinition definition) {
        super(definition, calculation);
        this.condition = definition.getCondition();
    }

    protected ValueMap getInputs() {
        if (inputs == null) {
            inputs = new ValueMap();
            for (SourceDefinition sourceDefinition : definition.getSources()) {
                Source source = calculation.getInstance(sourceDefinition);
                inputs.put(sourceDefinition.getIdentifier(), source.getResult().getValue());
            }
        }
        return inputs;
    }

    public boolean isValid() {
        if (condition == null) {
            return true;
        }
        return condition.getBooleanResult(calculation, this, getInputs());
    }

    protected Result calculateResult() {
        // Let the expression create a result for us
        return definition.getResult(calculation, this, getInputs());
    }
}
