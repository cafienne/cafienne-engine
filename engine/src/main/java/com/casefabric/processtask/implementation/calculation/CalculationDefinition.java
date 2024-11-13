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

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.instance.task.process.ProcessTask;
import com.casefabric.processtask.definition.InlineSubProcessDefinition;
import com.casefabric.processtask.implementation.calculation.definition.FilterStepDefinition;
import com.casefabric.processtask.implementation.calculation.definition.MapStepDefinition;
import com.casefabric.processtask.implementation.calculation.definition.MultiStepDefinition;
import com.casefabric.processtask.implementation.calculation.definition.StepDefinition;
import com.casefabric.processtask.implementation.calculation.definition.source.InputParameterSourceDefinition;
import com.casefabric.processtask.implementation.calculation.definition.source.SourceDefinition;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

public class CalculationDefinition extends InlineSubProcessDefinition {
    private final Collection<StepDefinition> steps = new ArrayList<>();
    private final Map<String, SourceDefinition> sources = new HashMap<>();

    public CalculationDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        parse("step", StepDefinition.class, steps);
        parse("filter", FilterStepDefinition.class, steps);
        parse("map", MapStepDefinition.class, steps);

        // Now, build the mapping chain; sources can be input parameters and also the mappings themselves
        getProcessDefinition().getInputParameters().forEach((name, parameter) -> sources.put(name, new InputParameterSourceDefinition(parameter)));

        // If multiple steps have the same name, they need to be combined into a MultiStep that will be evaluated in order (based on the condition)
        Set<String> stepIdentifiers = steps.stream().map(StepDefinition::getIdentifier).collect(Collectors.toSet());
        stepIdentifiers.forEach(identifier -> {
            List<StepDefinition> list = steps.stream().filter(step -> step.getIdentifier().equals(identifier)).collect(Collectors.toList());
            if (list.size() > 1) {
                sources.put(identifier, new MultiStepDefinition(this, identifier, list));
            } else if (list.size() == 1) {
                sources.put(identifier, list.get(0));
            }
        });

        // Make sure we have all output parameters covered for
        getProcessDefinition().getOutputParameters().forEach((name, parameter) -> {
            if (!sources.containsKey(name)) {
                processDefinition.addDefinitionError("Calculation Task '" + processDefinition.getName() + "' has an output parameter '" + name + "' but no mapping to fill it.");
            }
        });
    }

    public SourceDefinition getSource(String identifier) {
        return sources.get(identifier);
    }

    public SourceDefinition getTarget(String identifier) {
        return sources.get(identifier);
    }

    @Override
    public Calculation createInstance(ProcessTask processTaskActor) {
        return new Calculation(processTaskActor, this);
    }

    @Override
    protected Set<String> getRawOutputParameterNames() {
        return super.getExceptionParameterNames();
    }

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
