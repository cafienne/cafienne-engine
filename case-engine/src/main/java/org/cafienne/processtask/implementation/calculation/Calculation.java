/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.util.Map;

public class Calculation extends SubProcess<CalculationDefinition> {

    public Calculation(ProcessTaskActor processTask, CalculationDefinition definition) {
        super(processTask, definition);
    }

    public ProcessTaskActor getTask() {
        return super.processTaskActor;
    }

    @Override
    public void reactivate() {
        start(); // Just do the call again.
    }

    @Override
    public void start() {
        // Print debug information
        ProcessDefinition processDefinition = this.definition.getProcessDefinition();
        Map<String, OutputParameterDefinition> outputs = processDefinition.getOutputParameters();
        processTaskActor.addDebugInfo(() -> processDefinition.getName() + ": running " + outputs.size() + " calculations for output parameters");
        try {
            outputs.forEach((name, parameter) -> {
                processTaskActor.addDebugInfo(() -> "Calculating value for " + name);
                Value output = definition.getTarget(name).getValue(this);
                if (output != null && !Value.NULL.equals(output)) {
                    processTaskActor.addDebugInfo(() -> "Result for '" + name + "': ", output);
                    setProcessOutputParameter(name, output);
                } else {
                    processTaskActor.addDebugInfo(() -> "Result for '" + name + "' is null, hence it is not added the output parameters");
                }
            });
            raiseComplete();
        } catch (InvalidExpressionException e) {
            raiseFault("Failure during calculations", e);
        }
    }

    @Override
    public void suspend() {
    }

    @Override
    public void terminate() {
    }

    @Override
    public void resume() {
    }
}
