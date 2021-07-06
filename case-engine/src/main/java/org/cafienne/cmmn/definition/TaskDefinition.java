/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cafienne.cmmn.definition.parameter.TaskInputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.TaskOutputParameterDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

public abstract class TaskDefinition<T extends TaskImplementationContract> extends PlanItemDefinitionDefinition {
    private final boolean isBlocking;
    private final Map<String, TaskInputParameterDefinition> inputs = new LinkedHashMap<>();
    private final Map<String, TaskOutputParameterDefinition> outputs = new LinkedHashMap<>();
    private final Collection<ParameterMappingDefinition> mappings = new ArrayList<>();

    protected TaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        isBlocking = Boolean.valueOf(parseAttribute("isBlocking", false, "true")).booleanValue();

        parse("inputs", TaskInputParameterDefinition.class, inputs);
        parse("outputs", TaskOutputParameterDefinition.class, outputs);

        // A Task that is non-blocking (isBlocking set to 'false') MUST NOT have outputs.
        if (!isBlocking()) {
            if (!getOutputParameters().isEmpty()) {
                getCaseDefinition().addDefinitionError("The non blocking task " + getName() + " may not have output parameters");
            }
        }
        
        // We just implement this for all tasks, but in practice this is only done for Case- and ProcessTasks
        parse("parameterMapping", ParameterMappingDefinition.class, mappings);
    }

    /**
     * Returns whether the execution of this task should be done synchronously or asynchronously.
     *
     * @return
     */
    public boolean isBlocking() {
        return isBlocking;
    }

    /**
     * Returns the map with input parameters for this task.
     *
     * @return
     */
    public Map<String, TaskInputParameterDefinition> getInputParameters() {
        return inputs;
    }

    /**
     * Returns the map with output parameters for this task.
     *
     * @return
     */
    public Map<String, TaskOutputParameterDefinition> getOutputParameters() {
        return outputs;
    }

    /**
     * Returns the parameter mappings to be transformed upon activating and completing this task. Note that these mappings are only filled for CaseTasks and ProcessTasks, not for HumanTasks.
     *
     * @return
     */
    public Collection<ParameterMappingDefinition> getParameterMappings() {
        return mappings;
    }

    public Stream<ParameterMappingDefinition> getInputMappings() {
        return mappings.stream().filter(m -> m.isInputParameterMapping());
    }

    public Stream<ParameterMappingDefinition> getOutputMappings() {
        return mappings.stream().filter(m -> !m.isInputParameterMapping());
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Start;
    }

    /**
     * This method returns the definition for the actual task implementation
     * @return
     */
    abstract T getImplementationDefinition();
}
