/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.task;

import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
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
    private final Value<?> taskModel;
    private final DueDateDefinition dueDate;
    private final AssignmentDefinition assignment;

    public WorkflowTaskDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);

        parse("input", InputParameterDefinition.class, inputParameters);
        parse("output", OutputParameterDefinition.class, outputParameters);

        this.assignment = parse("assignment", AssignmentDefinition.class, false);
        this.dueDate = parse("duedate", DueDateDefinition.class, false);

        taskModel = parseTaskModel();

        // Parse custom parameter mappings into the (human) task definition
        final TaskDefinition<?> task = getParentElement();
        parse("parameterMapping", ParameterMappingDefinition.class, task.getParameterMappings());
    }

    public WorkflowTask createInstance(HumanTask humanTask) {
        return new WorkflowTask(this, humanTask);
    }

    /**
     * Returns the map with input parameters for this task.
     *
     * @return
     */
    public Map<String, InputParameterDefinition> getInputParameters() {
        return inputParameters;
    }

    /**
     * Returns the map with output parameters for this task.
     *
     * @return
     */
    public Map<String, OutputParameterDefinition> getOutputParameters() {
        return outputParameters;
    }

    /**
     * Get the task-model / Json schema for task
     *
     * @return task-model / Json schema for the task
     */
    public Value<?> getTaskModel() {
        return taskModel;
    }

    private Value<?> parseTaskModel() {
        String taskModelStr = parseString("task-model", false, "");

        try {
            return JSONReader.parse(taskModelStr);
        } catch (IOException | JSONParseFailure e) {
            return new StringValue(taskModelStr);
        }
    }

    public AssignmentDefinition getAssignmentExpression() {
        return assignment;
    }

    public DueDateDefinition getDueDateExpression() {
        return dueDate;
    }

    public static WorkflowTaskDefinition createEmptyDefinition(HumanTaskDefinition taskDefinition) {
        Element customTag = taskDefinition.getElement().getOwnerDocument().createElementNS(NAMESPACE_URI, "implementation");
        return new WorkflowTaskDefinition(customTag, taskDefinition.getModelDefinition(), taskDefinition);
    }
}
