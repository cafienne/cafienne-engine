/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.instance.task.process.ProcessTask;
import org.cafienne.processtask.definition.InlineSubProcessDefinition;
import org.cafienne.processtask.implementation.calculation.definition.FilterStepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.MapStepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.MultiStepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.InputParameterSourceDefinition;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;
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
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
