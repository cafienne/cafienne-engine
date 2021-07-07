/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.util.*;
import java.util.stream.Collectors;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ParameterMappingDefinition;
import org.cafienne.cmmn.definition.TaskDefinition;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.parameter.TaskOutputParameter;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskInputFilled;
import org.cafienne.cmmn.actorapi.event.plan.task.TaskOutputFilled;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class Task<D extends TaskDefinition<?>> extends PlanItem<D> {
    private ValueMap taskInput = new ValueMap();
    private ValueMap implementationInput = new ValueMap();

    private ValueMap givenOutput = new ValueMap();
    private ValueMap taskOutput = new ValueMap();

    protected Task(String id, int index, ItemDefinition itemDefinition, D definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage, StateMachine.TaskStage);
    }

    @Override
    abstract protected void createInstance();

    @Override
    final protected void completeInstance() {
        // Hmmm... can only be invoked by the implementation.
    }

    @Override
    abstract protected void terminateInstance();

    @Override
    final protected void startInstance() {
        transformInputParameters();
        startImplementation(implementationInput);
    }

    abstract protected void startImplementation(ValueMap inputParameters);

    @Override
    abstract protected void suspendInstance();

    @Override
    abstract protected void resumeInstance();

    @Override
    protected void reactivateInstance() {
        transformInputParameters();
        reactivateImplementation(implementationInput);
    }

    abstract protected void reactivateImplementation(ValueMap inputParameters);

    @Override
    final protected void failInstance() {
        // Hmmm... can only be invoked by the implementation.
    }

    protected void transformInputParameters() {
        ValueMap mappedInputParameters = new ValueMap();
        ValueMap inputParametersValues = new ValueMap();

        // First, create and set the parameter value based on the case file bindings
        Map<String, TaskInputParameter> inputParameters = new LinkedHashMap<>();
        getDefinition().getInputParameters().forEach((name, inputParameterDefinition) -> {
            TaskInputParameter inputParameter = new TaskInputParameter(inputParameterDefinition, this);
            inputParameters.put(name, inputParameter);
            inputParametersValues.put(name, inputParameter.getValue());
        });

        // Next, run the transformations on the parameters to fill the values for the task implementation parameters.
        final Collection<ParameterMappingDefinition> mappings = this.getDefinition().getParameterMappings();
        for (ParameterMappingDefinition mapping : mappings) {
            if (mapping.isInputParameterMapping()) {
                TaskInputParameter taskParameter = inputParameters.get(mapping.getSource().getName());
                Value<?> value = mapping.transformInput(this, taskParameter);

                mappedInputParameters.put(mapping.getTarget().getName(), value);
            }
        }

        addEvent(new TaskInputFilled(this, inputParametersValues, mappedInputParameters));
    }

    public ValidationResponse validateOutput(ValueMap implementationOutput) {
        // Run mappings will throw the InvalidCommandException if there are errors.
        transformOutputParameters(implementationOutput, true);
        // Everything went ok, let's return an empty value map
        return new ValidationResponse(new ValueMap());
    }

    /**
     * This method is called to complete a task
     * @param rawOutputParameters
     */
    public void goComplete(ValueMap rawOutputParameters) {
        makeTransitionWithOutput(rawOutputParameters, Transition.Complete);
    }

    /**
     * This method is called when a task fails
     * @param rawOutputParameters
     */
    public void goFault(ValueMap rawOutputParameters) {
        makeTransitionWithOutput(rawOutputParameters, Transition.Fault);
    }

    private void makeTransitionWithOutput(ValueMap rawOutputParameters, Transition transition) {
        prepareTransition(transition);
        ValueMap newTaskOutput = transformOutputParameters(rawOutputParameters, transition == Transition.Complete);
        addEvent(new TaskOutputFilled(this, newTaskOutput, rawOutputParameters));
        makeTransition(transition);
    }

    /**
     * Fill the output parameters of the task, based on the output parameters of the implementation
     * of the task (i.e., based on the output of the sub process or sub case that was invoked).
     *
     * @param output
     * @param validateOutput Indicates whether a check on mandatory parameter values must be done
     */
    protected ValueMap transformOutputParameters(ValueMap output, boolean validateOutput) {
        ValueMap taskImplementationOutput = output == null ? new ValueMap() : output;

        // Create maps to store the values that must be validated.
        Collection<ParameterMappingDefinition> mappings = getDefinition().getParameterMappings();

        // Check on missing raw parameters and log debug information for that.
        addDebugInfo(() -> {
            List<ParameterMappingDefinition> missingParameters = mappings.stream().filter(m -> !m.isInputParameterMapping() && !taskImplementationOutput.has(m.getSource().getName())).collect(Collectors.toList());
            if (missingParameters.isEmpty()) {
                return "";
            } else if (missingParameters.size() == 1) {
                return "Task Output: parameter " + missingParameters.get(0).getTarget().getName() +" has no value, because raw parameter " + missingParameters.get(0).getSource().getName() + " is missing";
            } else {
                List<String> missingRawParameters = missingParameters.stream().map(m -> m.getSource().getName()).collect(Collectors.toList());
                List<String> missingTaskParameters = missingParameters.stream().map(m -> m.getTarget().getName()).collect(Collectors.toList());
                return "Task Output: parameters " + missingTaskParameters +" have no value - missing raw output parameters " + missingRawParameters;
            }
        });
        // Check on raw parameters that have no matching definition
        addDebugInfo(() -> {
            List<String> undefinedParameters = taskImplementationOutput.getValue().keySet().stream().filter(rawOutputParameterName -> mappings.stream().filter(m -> m.getSource().getName().equals(rawOutputParameterName)).count() == 0).collect(Collectors.toList());
            if (undefinedParameters.isEmpty()) {
                return "";
            } else if (undefinedParameters.size() == 1) {
                return "Task Output: found parameter " + undefinedParameters.get(0) +", but it is not defined";
            } else {
                return "Task Output: found parameters " + undefinedParameters + ", but they are not defined";
            }
        });

        // First, transform values of all raw output parameters into task parameters
        ValueMap newTaskOutput = new ValueMap();
        for (ParameterMappingDefinition mapping : mappings) {
            if (!mapping.isInputParameterMapping()) {
                String rawOutputParameterName = mapping.getSource().getName();
                String taskOutputParameterName = mapping.getTarget().getName();
                // If the raw output parameter is missing, it makes no sense to execute a mapping
                //  This is typically the case for e.g. failing process tasks
                //  Note that there is an exception for mappings that have a "static" expression, i.e.,
                //  an expression that does not rely on the raw output parameter.
                if (taskImplementationOutput.has(rawOutputParameterName)) {
                    Value<?> rawValue = taskImplementationOutput.get(rawOutputParameterName);
                    Value<?> taskOutputParameterValue = mapping.transformOutput(this, rawValue);
                    newTaskOutput.put(taskOutputParameterName, taskOutputParameterValue);
                } else if (mapping.hasTransformation()) {
                    // If the raw output parameter is missing, but a transformation is defined,
                    //  let's try to execute the transformation with a null value.
                    // If a null value is returned, then we should not create a new output parameter.
                    // However, for "static" expressions, e.g. that return a task.name or case.id without needing a value,
                    // an outcome different than Value.NULL may come. In such cases, we will also create the output
                    // parameter with that new value.
                    Value<?> taskOutputParameterValue = mapping.transformOutput(this, Value.NULL);
                    if (! Value.NULL.equals(taskOutputParameterValue)) {
                        newTaskOutput.put(taskOutputParameterName, taskOutputParameterValue);
                    }
                } else {
                    // In this case, the expected raw output parameter is not available with a value,
                    //  and there is also no static expression to fill the target task output parameter.
                    // Therefore, we will omit the target task output parameter (i.e., we do NOT pass e.g. a null value)
                    // The consequence is that the case file will not be filled/overwritten with e.g. unexpected null values.
                }
            }
        }

        // Now check that all mandatory parameters have a non-null value
        if (validateOutput) {
            Vector<String> validationErrors = new Vector();
            getDefinition().getOutputParameters().forEach((name, parameterDefinition) -> {
                if (parameterDefinition.isMandatory()) {
//                    System.out.println("Validating mandatory output parameter "+name);
                    Value<?> parameterValue = newTaskOutput.get(name);
                    if (parameterValue.equals(Value.NULL)) {
                        validationErrors.add("Task output parameter "+ name+" does not have a value, but that is required in order to complete the task");
                    }
                }
            });

            if (validationErrors.size() > 0) {
                throw new InvalidCommandException(validationErrors.toString());
            }
        }

        return newTaskOutput;
    }

    protected ValueMap getInputParameters() {
        return taskInput;
    }

    /**
     * Returns the mapped input parameters for task
     * @return mapped input parameters for task
     */
    public ValueMap getMappedInputParameters() {
        return implementationInput;
    }

    @Override
    protected void dumpImplementationToXML(Element planItemXML) {
        super.dumpImplementationToXML(planItemXML);
        taskInput.fieldNames().forEachRemaining(fieldName -> appendParameter(fieldName, taskInput.get(fieldName), planItemXML, "inputs"));
        taskOutput.fieldNames().forEachRemaining(fieldName -> appendParameter(fieldName, taskOutput.get(fieldName), planItemXML, "outputs"));
    }

    private void appendParameter(String parameterName, Value<?> value, Element planItemXML, String tagName) {
        Element paramXML = planItemXML.getOwnerDocument().createElement(tagName);
        planItemXML.appendChild(paramXML);
        paramXML.setAttribute("name", parameterName);
        Node valueNode = planItemXML.getOwnerDocument().createTextNode(String.valueOf(value));
        paramXML.appendChild(valueNode);
    }

    public void updateState(TaskInputFilled event) {
        this.taskInput = event.getTaskInputParameters();
        this.implementationInput = event.getMappedInputParameters();
    }

    public void updateState(TaskOutputFilled event) {
        this.givenOutput = event.getRawOutputParameters();
        this.taskOutput = event.getTaskOutputParameters();
        this.taskOutput.getValue().forEach((name, value) -> {
            // Note, this code assumes all task output keys have a correspondingly defined parameter.
            ParameterDefinition definition = getDefinition().getOutputParameters().get(name);
            TaskOutputParameter outputParameter = new TaskOutputParameter(definition, this, value);
            outputParameter.bind();
        });
    }
}
