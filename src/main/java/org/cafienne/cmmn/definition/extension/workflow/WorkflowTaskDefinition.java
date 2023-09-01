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

package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.json.JSONParseFailure;
import org.cafienne.json.JSONReader;
import org.cafienne.json.StringValue;
import org.cafienne.json.Value;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkflowTaskDefinition extends CMMNElementDefinition implements TaskImplementationContract {
    private final Map<String, InputParameterDefinition> inputParameters = new LinkedHashMap<>();
    private final Map<String, OutputParameterDefinition> outputParameters = new LinkedHashMap<>();
    private final TaskModelDefinition taskModel;
    private final DueDateDefinition dueDate;
    private final AssignmentDefinition assignment;

    public WorkflowTaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);

        parse("input", InputParameterDefinition.class, inputParameters);
        parse("output", OutputParameterDefinition.class, outputParameters);

        this.assignment = parse("assignment", AssignmentDefinition.class, false);
        this.dueDate = parse("duedate", DueDateDefinition.class, false);
        this.taskModel = parse("task-model", TaskModelDefinition.class, false);

        // Parse custom parameter mappings into the (human) task definition
        final TaskDefinition<?> task = getParentElement();
        parse("parameterMapping", ParameterMappingDefinition.class, task.getParameterMappings());
    }

    public WorkflowTask createInstance(HumanTask humanTask) {
        return new WorkflowTask(this, humanTask);
    }

    /**
     * Returns the map with input parameters for this task.
     */
    public Map<String, InputParameterDefinition> getInputParameters() {
        return inputParameters;
    }

    /**
     * Returns the map with output parameters for this task.
     */
    public Map<String, OutputParameterDefinition> getOutputParameters() {
        return outputParameters;
    }

    /**
     * Get the task-model / Json schema for task
     *
     * @return task-model / Json schema for the task
     */
    public TaskModelDefinition getTaskModel() {
        return taskModel;
    }

    public AssignmentDefinition getAssignmentExpression() {
        return assignment;
    }

    public DueDateDefinition getDueDateExpression() {
        return dueDate;
    }

    public static WorkflowTaskDefinition createEmptyDefinition(HumanTaskDefinition taskDefinition) {
        Element customTag = taskDefinition.getElement().getOwnerDocument().createElementNS(CAFIENNE_NAMESPACE, "implementation");
        return new WorkflowTaskDefinition(customTag, taskDefinition.getModelDefinition(), taskDefinition);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameWorkflow);
    }

    public boolean sameWorkflow(WorkflowTaskDefinition other) {
        return same(taskModel, other.taskModel)
                && same(dueDate, other.dueDate)
                && same(assignment, other.assignment)
                && same(inputParameters.values(), other.inputParameters.values())
                && same(outputParameters.values(), other.outputParameters.values());
    }
}
