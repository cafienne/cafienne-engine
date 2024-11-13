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

package com.casefabric.processtask.implementation.calculation;

import com.casefabric.cmmn.definition.parameter.OutputParameterDefinition;
import com.casefabric.cmmn.expression.InvalidExpressionException;
import com.casefabric.cmmn.instance.task.process.ProcessTask;
import com.casefabric.json.Value;
import com.casefabric.processtask.definition.ProcessDefinition;
import com.casefabric.processtask.implementation.InlineSubProcess;
import com.casefabric.processtask.implementation.calculation.definition.source.SourceDefinition;
import com.casefabric.processtask.implementation.calculation.operation.Source;

import java.util.HashMap;
import java.util.Map;

public class Calculation extends InlineSubProcess<CalculationDefinition> {
    private final Map<SourceDefinition, Source<?>> sources = new HashMap<>();

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
                Source<?> step = getSource(name);
                if (step.isValid()) {
                    Result result = step.getResult();
                    Value<?> output = result.getValue();
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

    Source<?> getSource(String target) {
        return getSource(definition.getTarget(target));
    }

    public Source<?> getSource(SourceDefinition definition) {
        Source<?> source = sources.get(definition);
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
