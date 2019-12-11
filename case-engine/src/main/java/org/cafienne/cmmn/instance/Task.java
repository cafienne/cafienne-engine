/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.cafienne.cmmn.definition.ParameterMappingDefinition;
import org.cafienne.cmmn.definition.TaskDefinition;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.cmmn.instance.parameter.TaskOutputParameter;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.cmmn.akka.event.TaskInputFilled;
import org.cafienne.cmmn.akka.event.TaskOutputFilled;
import org.cafienne.cmmn.instance.task.validation.ValidationResponse;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class Task<D extends TaskDefinition<?>> extends PlanItemDefinitionInstance<D> {
    private ValueMap taskInputParameters = new ValueMap();
    private ValueMap taskOutputParameters = new ValueMap();
    private ValueMap mappedInputParameters = new ValueMap();

    protected Task(PlanItem planItem, D definition) {
        super(planItem, definition, StateMachine.TaskStage);
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
        startImplementation(mappedInputParameters);
    }

    abstract protected void startImplementation(ValueMap inputParameters);

    @Override
    abstract protected void suspendInstance();

    @Override
    abstract protected void resumeInstance();

    @Override
    protected void reactivateInstance() {
        transformInputParameters();
        reactivateImplementation(mappedInputParameters);
    }

    abstract protected void reactivateImplementation(ValueMap inputParameters);

    @Override
    final protected void failInstance() {
        // Hmmm... can only be invoked by the implementation.
    }

    public ValidationResponse validateOutput(ValueMap potentialRawOutput) {
        taskOutputParameters.getValue().clear(); // Make sure to clear, to avoid repeated calls re-using the earlier mapped output

        if (potentialRawOutput == null) {
            potentialRawOutput = new ValueMap();
        }

        Vector<String> validationErrors = new Vector<>();

        Collection<ParameterMappingDefinition> mappings = getDefinition().getParameterMappings();
        for (ParameterMappingDefinition mapping : mappings) {
            if (!mapping.isInputParameterMapping()) {
                String rawOutputParameterName = mapping.getSource().getName();
                Value<?> value = potentialRawOutput.get(rawOutputParameterName);
                TaskOutputParameter outputParameter = mapping.transformOutput(this, value);
                taskOutputParameters.put(outputParameter.getDefinition().getName(), outputParameter.getValue());
            }
        }

        getDefinition().getOutputParameters().forEach((name, parameterDefinition) -> {
            if (parameterDefinition.isMandatory()) {
                Value<?> parameterValue = taskOutputParameters.get(name);
                if (parameterValue.equals(Value.NULL)) {
                    validationErrors.add("Task output parameter "+ name+" does not have a value, but that is required in order to complete the task");
                }
            }
        });

        if (validationErrors.size() > 0) {
            throw new InvalidCommandException(validationErrors.toString());
        }

        taskOutputParameters.getValue().clear(); // Make sure to clear, to avoid results of
        return new ValidationResponse(taskOutputParameters);
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
        getPlanItem().prepareTransition(transition);
        transformOutputParameters(rawOutputParameters, transition == Transition.Complete);
        getPlanItem().makeTransition(transition);
    }

    protected void transformInputParameters() {
        mappedInputParameters = new ValueMap();

        Map<String, TaskInputParameter> inputParameters = new LinkedHashMap<>();
        getDefinition().getInputParameters().forEach((name, inputParameterDefinition) -> {
            TaskInputParameter inputParameter = new TaskInputParameter(inputParameterDefinition, getCaseInstance());
            inputParameters.put(name, inputParameter);
            taskInputParameters.put(name, inputParameter.getValue());
        });

        final Collection<ParameterMappingDefinition> mappings = this.getDefinition().getParameterMappings();
        for (ParameterMappingDefinition mapping : mappings) {
            if (mapping.isInputParameterMapping()) {
                TaskInputParameter taskParameter = inputParameters.get(mapping.getSource().getName());
                Value<?> value = mapping.transformInput(this, taskParameter);

                mappedInputParameters.put(mapping.getTarget().getName(), value);
            }
        }

        final TaskInputFilled event = new TaskInputFilled(this, taskInputParameters, mappedInputParameters);
        getCaseInstance().storeInternallyGeneratedEvent(event);
        event.finished();
    }

    /**
     * Fill the output parameters of the task, based on the output parameters of the implementation
     * of the task (i.e., based on the output of the sub process or sub case that was invoked).
     *
     * @param rawOutputParameters
     * @param validateOutput Indicates whether a check on mandatory parameter values must be done
     */
    protected void transformOutputParameters(ValueMap rawOutputParameters, boolean validateOutput) {
        if (rawOutputParameters == null) {
            rawOutputParameters = new ValueMap();
        }
        
        Collection<ParameterMappingDefinition> mappings = getDefinition().getParameterMappings();
        for (ParameterMappingDefinition mapping : mappings) {
            if (!mapping.isInputParameterMapping()) {
                String rawOutputParameterName = mapping.getSource().getName();
                Value<?> value = rawOutputParameters.get(rawOutputParameterName);
                TaskOutputParameter outputParameter = mapping.transformOutput(this, value);
                outputParameter.bind();
                taskOutputParameters.put(outputParameter.getDefinition().getName(), outputParameter.getValue());
            }
        }

        if (validateOutput) {
            getDefinition().getOutputParameters().forEach((name, parameterDefinition) -> {
                if (parameterDefinition.isMandatory()) {
//                    System.out.println("Validating mandatory output parameter "+name);
                    Value<?> parameterValue = taskOutputParameters.get(name);
//                    System.out.println(" Value is of type "+parameterValue.getClass().getSimpleName()+", holding\n"+parameterValue);
                    if (parameterValue.equals(Value.NULL)) {
//                        System.out.println("Raising invalid");
                        throw new InvalidCommandException("Task output parameter "+ name+" does not have a value, but that is required in order to complete the task");
                    }
                } else {
//                    System.out.println("Output parameter "+name+" is not mandatory");

                }
            });
        }

        TaskOutputFilled event = new TaskOutputFilled(this, taskOutputParameters, rawOutputParameters);
        getCaseInstance().storeInternallyGeneratedEvent(event);
        event.finished();
    }

    protected ValueMap getInputParameters() {
        return taskInputParameters;
    }

    /**
     * Returns the mapped input parameters for task
     * @return mapped input parameters for task
     */
    public ValueMap getMappedInputParameters() {
        return mappedInputParameters;
    }

    @Override
    protected void dumpMemoryStateToXML(Element planItemXML) {
        super.dumpMemoryStateToXML(planItemXML);
        taskInputParameters.fieldNames().forEachRemaining(fieldName -> appendParameter(fieldName, taskInputParameters.get(fieldName), planItemXML, "inputs"));
        taskOutputParameters.fieldNames().forEachRemaining(fieldName -> appendParameter(fieldName, taskOutputParameters.get(fieldName), planItemXML, "outputs"));
    }

    private void appendParameter(String parameterName, Value<?> value, Element planItemXML, String tagName) {
        Element paramXML = planItemXML.getOwnerDocument().createElement(tagName);
        planItemXML.appendChild(paramXML);
        paramXML.setAttribute("name", parameterName);
        Node valueNode = planItemXML.getOwnerDocument().createTextNode(String.valueOf(value));
        paramXML.appendChild(valueNode);
    }

    public void recoverTaskEvent(TaskInputFilled event) {
        this.taskInputParameters = event.getTaskInputParameters();
        this.mappedInputParameters = event.getMappedInputParameters();
    }

    public void recoverTaskEvent(TaskOutputFilled event) {
        this.taskOutputParameters = event.getTaskOutputParameters();
    }

}
