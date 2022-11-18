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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.parameter.TaskInputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.TaskOutputParameterDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public abstract class TaskDefinition<T extends TaskImplementationContract> extends PlanItemDefinitionDefinition {
    private final boolean isBlocking;
    private final Map<String, TaskInputParameterDefinition> inputs = new LinkedHashMap<>();
    private final Map<String, TaskOutputParameterDefinition> outputs = new LinkedHashMap<>();
    private final Collection<ParameterMappingDefinition> mappings = new ArrayList<>();

    protected TaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        isBlocking = Boolean.parseBoolean(parseAttribute("isBlocking", false, "true"));

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
        return mappings.stream().filter(ParameterMappingDefinition::isInputParameterMapping);
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
     *
     * @return
     */
    abstract T getImplementationDefinition();

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameTask);
    }

    public boolean sameTask(TaskDefinition<T> other) {
        return samePlanItemDefinitionDefinition(other)
                && same(isBlocking, other.isBlocking)
                && sameInputParameters(other)
                && sameOutputParameters(other)
                && sameMappings(other);
    }

    public boolean sameInputParameters(TaskDefinition<T> other) {
        return same(inputs.values(), other.inputs.values());
    }

    public boolean sameOutputParameters(TaskDefinition<T> other) {
        return same(outputs.values(), other.outputs.values());
    }

    public boolean sameMappings(TaskDefinition<?> other) {
        return same(mappings, other.mappings);
    }
}
