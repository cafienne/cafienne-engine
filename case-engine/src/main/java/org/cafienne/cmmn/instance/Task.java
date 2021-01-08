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
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.parameter.ParameterDefinition;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.parameter.TaskOutputParameter;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.cmmn.akka.event.plan.task.TaskInputFilled;
import org.cafienne.cmmn.akka.event.plan.task.TaskOutputFilled;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class Task<D extends TaskDefinition<?>> extends PlanItem<D> {
    private ValueMap taskInput = new ValueMap();
    private ValueMap implementationInput = new ValueMap();

    private ValueMap implementationOutput = new ValueMap();
    private ValueMap taskOutput = new ValueMap();

    protected Task(String id, int index, ItemDefinition itemDefinition, D definition, Stage stage) {
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

    public ValidationResponse validateOutput(ValueMap potentialRawOutput) {
        // Create maps to store the values that must be validated.
        ValueMap implementationOutput = potentialRawOutput == null ? new ValueMap() : potentialRawOutput;
        ValueMap potentialTaskOutput = new ValueMap();
        Vector<String> validationErrors = new Vector();

        Collection<ParameterMappingDefinition> mappings = getDefinition().getParameterMappings();
        for (ParameterMappingDefinition mapping : mappings) {
            if (!mapping.isInputParameterMapping()) {
                String implementationOutputParameterName = mapping.getSource().getName();
                Value<?> implementationOutputParameterValue = implementationOutput.get(implementationOutputParameterName);
                Value<?> outputParameter = mapping.transformOutput(this, implementationOutputParameterValue);
                potentialTaskOutput.put(mapping.getTarget().getName(), outputParameter);
            }
        }

        getDefinition().getOutputParameters().forEach((name, parameterDefinition) -> {
            if (parameterDefinition.isMandatory()) {
                Value<?> parameterValue = potentialTaskOutput.get(name);
                if (parameterValue.equals(Value.NULL)) {
                    validationErrors.add("Task output parameter "+ name+" does not have a value, but that is required in order to complete the task");
                }
            }
        });

        if (validationErrors.size() > 0) {
            throw new InvalidCommandException(validationErrors.toString());
        }

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
        if (rawOutputParameters == null) {
            rawOutputParameters = new ValueMap();
        }
        transformOutputParameters(rawOutputParameters, transition == Transition.Complete);
        makeTransition(transition);
    }

    protected void transformInputParameters() {
        ValueMap mappedInputParameters = new ValueMap();
        ValueMap inputParametersValues = new ValueMap();

        Map<String, TaskInputParameter> inputParameters = new LinkedHashMap();
        getDefinition().getInputParameters().forEach((name, inputParameterDefinition) -> {
            TaskInputParameter inputParameter = new TaskInputParameter(inputParameterDefinition, this);
            inputParameters.put(name, inputParameter);
            inputParametersValues.put(name, inputParameter.getValue());
        });

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

    /**
     * Fill the output parameters of the task, based on the output parameters of the implementation
     * of the task (i.e., based on the output of the sub process or sub case that was invoked).
     *
     * @param implementationOutput
     * @param validateOutput Indicates whether a check on mandatory parameter values must be done
     */
    protected void transformOutputParameters(ValueMap implementationOutput, boolean validateOutput) {
        Collection<ParameterMappingDefinition> mappings = getDefinition().getParameterMappings();

        // Check on missing raw parameters
        addDebugInfo(() -> {
            List<ParameterMappingDefinition> missingParameters = mappings.stream().filter(m -> !m.isInputParameterMapping() && !implementationOutput.has(m.getSource().getName())).collect(Collectors.toList());
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
            List<String> undefinedParameters = implementationOutput.getValue().keySet().stream().filter(rawOutputParameterName -> mappings.stream().filter(m -> m.getSource().getName().equals(rawOutputParameterName)).count() == 0).collect(Collectors.toList());
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
                if (implementationOutput.has(rawOutputParameterName)) {
                    Value<?> rawValue = implementationOutput.get(rawOutputParameterName);
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
                }
            }
        }

        // Now check that all mandatory parameters have a non-null value
        if (validateOutput) {
            getDefinition().getOutputParameters().forEach((name, parameterDefinition) -> {
                if (parameterDefinition.isMandatory()) {
//                    System.out.println("Validating mandatory output parameter "+name);
                    Value<?> parameterValue = newTaskOutput.get(name);
                    if (parameterValue.equals(Value.NULL)) {
                        throw new InvalidCommandException("Task output parameter "+ name+" does not have a value, but that is required in order to complete the task");
                    }
                }
            });
        }

        addEvent(new TaskOutputFilled(this, newTaskOutput, implementationOutput));
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
        this.implementationOutput = event.getRawOutputParameters();
        this.taskOutput = event.getTaskOutputParameters();
        this.taskOutput.getValue().forEach((name, value) -> {
            // Note, this code assumes all task output keys have a correspondingly defined parameter.
            ParameterDefinition definition = getDefinition().getOutputParameters().get(name);
            TaskOutputParameter outputParameter = new TaskOutputParameter(definition, this, value);
            outputParameter.bind();
        });
    }
}
