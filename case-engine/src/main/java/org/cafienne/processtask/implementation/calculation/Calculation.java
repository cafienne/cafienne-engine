/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation;

import org.cafienne.json.Value;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.instance.task.process.ProcessTask;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.InlineSubProcess;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;
import org.cafienne.processtask.implementation.calculation.operation.Source;

import java.util.HashMap;
import java.util.Map;

public class Calculation extends InlineSubProcess<CalculationDefinition> {
    private final Map<SourceDefinition, Source> sources = new HashMap();

    public Calculation(ProcessTask processTask, CalculationDefinition definition) {
        super(processTask, definition);
    }

    public ProcessTask getTask() {
        return super.task;
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
        addDebugInfo(() -> processDefinition.getName() + ": running " + outputs.size() + " calculations for output parameters");
        try {
            outputs.forEach((name, parameter) -> {
                addDebugInfo(() -> "Calculating value for " + name);
                Source step = getSource(name);
                if (step.isValid()) {
                    Result result = step.getResult();
                    Value output = result.getValue();
                    getTask().getCaseInstance().addDebugInfo(() -> "Result for '" + name + "': ", output);
                    setProcessOutputParameter(name, output);
                } else {
                    addDebugInfo(() -> "Result for '" + name + "' is not applicable, hence not added to the output parameters");
                }
            });
            raiseComplete();
        } catch (InvalidExpressionException e) {
            raiseFault("Failure during calculations", e);
        }
    }

    Source getSource(String target) {
        return getSource(definition.getTarget(target));
    }

    public Source getSource(SourceDefinition definition) {
        Source source = sources.get(definition);
        if (source == null) {
            source = definition.createInstance(this);
            sources.put(definition, source);
        }
        return source;
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
