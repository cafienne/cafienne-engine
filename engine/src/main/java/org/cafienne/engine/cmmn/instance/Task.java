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

package org.cafienne.engine.cmmn.instance;

import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.engine.cmmn.actorapi.event.plan.task.*;
import org.cafienne.engine.cmmn.definition.ItemDefinition;
import org.cafienne.engine.cmmn.definition.ParameterMappingDefinition;
import org.cafienne.engine.cmmn.definition.TaskDefinition;
import org.cafienne.engine.cmmn.definition.casefile.CaseFileError;
import org.cafienne.engine.cmmn.definition.parameter.TaskOutputParameterDefinition;
import org.cafienne.engine.cmmn.instance.parameter.TaskInputParameter;
import org.cafienne.engine.cmmn.instance.parameter.TaskOutputParameter;
import org.cafienne.engine.cmmn.instance.task.validation.ValidationResponse;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Task<D extends TaskDefinition<?>> extends TaskStage<D> {
    private ValueMap taskInput = new ValueMap();
    private ValueMap implementationInput = new ValueMap();

    private ValueMap taskOutput = new ValueMap();

    private final TaskImplementationActorState implementationState = new TaskImplementationActorState(this);

    protected Task(String id, int index, ItemDefinition itemDefinition, D definition, Stage<?> stage) {
        super(id, index, itemDefinition, definition, stage.getCaseInstance(), stage);
    }

    @FunctionalInterface
    interface TransitionHandler {
        void handle();
    }

    @Override
    final protected void completeInstance() {
        // Hmmm... can only be invoked by the implementation.
    }

    public final void handleImplementationTransition(Transition transition) {
        handlingChildTransition = true;
        makeTransition(transition);
        handlingChildTransition = false;
    }

    private boolean handlingChildTransition = false;

    @Override
    final protected void startInstance() {
        transformInputParameters();
        startImplementation(getMappedInputParameters());
    }

    abstract protected void startImplementation(ValueMap inputParameters);

    private void handleTransition(TransitionHandler handler, Transition transition) {
        if (handlingChildTransition) {
            addDebugInfo(() -> "Updated " + this + " on transition " + transition + " that happened in the implementation");
        } else {
            handler.handle();
        }
    }

    @Override
    final protected void suspendInstance() {
        handleTransition(this::suspendImplementation, Transition.Suspend);
    }

    abstract protected void suspendImplementation();

    @Override
    final protected void resumeInstance() {
        handleTransition(this::resumeImplementation, Transition.Resume);
    }

    abstract protected void resumeImplementation();

    @Override
    protected final void reactivateInstance() {
//        implementationState.reactivate();
        handleTransition(() -> {
            transformInputParameters();
            reactivateImplementation(getMappedInputParameters());
//            if (implementationState.isStarted()) {
//            } else {
//                addDebugInfo(() -> "Implementation of task " + this + " was not started yet, probably due to failures. Starting again");
//                startInstance();
//            }
        }, Transition.Reactivate);
    }

    abstract protected void reactivateImplementation(ValueMap inputParameters);

    @Override
    final protected void terminateInstance() {
        handleTransition(this::terminateImplementation, Transition.Terminate);
    }

    abstract protected void terminateImplementation();

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
     */
    public boolean goComplete(ValueMap rawOutputParameters) {
        return makeTransitionWithOutput(Transition.Complete, rawOutputParameters);
    }

    /**
     * This method is called when a task fails
     */
    public void goFault(ValueMap rawOutputParameters) {
        makeTransitionWithOutput(Transition.Fault, rawOutputParameters);
    }

    private boolean makeTransitionWithOutput(Transition transition, ValueMap rawOutputParameters) {
        prepareTransition(transition);
        if (rawOutputParameters != null) {
            ValueMap newTaskOutput = transformOutputParameters(rawOutputParameters, transition == Transition.Complete);
            addEvent(new TaskOutputFilled(this, newTaskOutput, rawOutputParameters));
        }
        return super.makeTransition(transition);
    }

    /**
     * Fill the output parameters of the task, based on the output parameters of the implementation
     * of the task (i.e., based on the output of the sub process or sub case that was invoked).
     *
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
                return "Task Output: parameter " + missingParameters.get(0).getTarget().getName() + " has no value, because raw parameter " + missingParameters.get(0).getSource().getName() + " is missing";
            } else {
                List<String> missingRawParameters = missingParameters.stream().map(m -> m.getSource().getName()).collect(Collectors.toList());
                List<String> missingTaskParameters = missingParameters.stream().map(m -> m.getTarget().getName()).collect(Collectors.toList());
                return "Task Output: parameters " + missingTaskParameters + " have no value - missing raw output parameters " + missingRawParameters;
            }
        });
        // Check on raw parameters that have no matching definition
        addDebugInfo(() -> {
            List<String> undefinedParameters = taskImplementationOutput.getValue().keySet().stream().filter(rawOutputParameterName -> mappings.stream().noneMatch(m -> m.getSource().getName().equals(rawOutputParameterName))).collect(Collectors.toList());
            if (undefinedParameters.isEmpty()) {
                return "";
            } else if (undefinedParameters.size() == 1) {
                return "Task Output: found parameter " + undefinedParameters.get(0) + ", but it is not defined";
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
                    if (!Value.NULL.equals(taskOutputParameterValue)) {
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

        // Now check that all mandatory parameters have a non-null value and also that binding to case file is proper
        if (validateOutput) {
            Vector<String> validationErrors = new Vector<>();
            getDefinition().getOutputParameters().forEach((name, parameterDefinition) -> {
                Value<?> parameterValue = newTaskOutput.get(name);
                if (parameterDefinition.isMandatory()) {
//                    System.out.println("Validating mandatory output parameter "+name);
                    if (parameterValue.equals(Value.NULL)) {
                        validationErrors.add("Task output parameter " + name + " does not have a value, but that is required in order to complete the task");
                    }
                }

                TaskOutputParameter outputParameter = new TaskOutputParameter(parameterDefinition, this, parameterValue);
                try {
                    outputParameter.validate(); // Validate each property
                } catch (CaseFileError | TransitionDeniedException invalid) {
                    validationErrors.add(invalid.getMessage());
                }
            });

            if (!validationErrors.isEmpty()) {
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
     *
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

    public void updateState(TaskImplementationStarted event) {
        getImplementationState().updateState(event);
    }

    public void updateState(TaskImplementationNotStarted event) {
        getImplementationState().updateState(event);
    }

    public void updateState(TaskImplementationReactivated event) {
        getImplementationState().updateState(event);
    }

    public void updateState(TaskCommandRejected event) {
        getImplementationState().updateState(event);
    }

    public void updateState(TaskInputFilled event) {
        this.taskInput = event.getTaskInputParameters();
        this.implementationInput = event.getMappedInputParameters();
    }

    public void updateState(TaskOutputFilled event) {
        this.taskOutput = event.getTaskOutputParameters();
        if (getCaseInstance().recoveryRunning()) {
            // No need to bind task output to the case file, as case file events will take care of setting right values
            return;
        }
        this.taskOutput.getValue().forEach((name, value) -> {
            // Note, this code assumes all task output keys have a correspondingly defined parameter.
            TaskOutputParameterDefinition definition = getDefinition().getOutputParameters().get(name);
            TaskOutputParameter outputParameter = new TaskOutputParameter(definition, this, value);
            outputParameter.bind();
        });
    }

    @Override
    protected void migrateItemDefinition(ItemDefinition newItemDefinition, D newDefinition, boolean skipLogic) {
        super.migrateItemDefinition(newItemDefinition, newDefinition, skipLogic);
        if (hasNewMappings()) {
            if (getState().isAlive()) {
                addDebugInfo(() -> "Found new mapping definitions in task " + this + ", transforming input parameters");
                transformInputParameters();
            }
        }
    }

    private boolean hasNewMappings() {
        return !getPreviousDefinition().sameMappings(getDefinition());
    }

    public void startTaskImplementation(ModelCommand command) {
        implementationState.sendRequest(command);
//        getCaseInstance().informImplementation(command, failure -> {
//            System.out.println("TASK GOT REJECTED");
//
//            getCaseInstance().addEvent(new TaskImplementationNotStarted(this, command, failure.toJson()));
//        }, success -> {
//            getCaseInstance().addEvent(new TaskImplementationStarted(this));
//            if (!getDefinition().isBlocking()) {
//                goComplete(new ValueMap());
//            }
//        });
    }

    public void reactivateTaskImplementation(ModelCommand command) {
        implementationState.sendRequest(command);
//        getCaseInstance().informImplementation(command, failure -> {
//            getCaseInstance().addEvent(new TaskCommandRejected(this, command, failure.toJson()));
//        }, success -> {
//            getCaseInstance().addEvent(new TaskImplementationReactivated(this));
//        });
    }

    public void tellTaskImplementation(ModelCommand command) {
        if (!getDefinition().isBlocking()) {
            return;
        }
        implementationState.sendRequest(command);
//        getCaseInstance().informImplementation(command, failure -> {
//            System.out.println("TASK GOT REJECTED");
//            getCaseInstance().addEvent(new TaskCommandRejected(this, command, failure.toJson()));
//        }, null);
    }

    public void giveNewDefinition(ModelCommand command) {
        if (getState().isInitiated()) {
            tellTaskImplementation(command);
//            if (getImplementationState().isStarted()) {
//                getCaseInstance().addDebugInfo(() -> this + ": informing task implementation about new definition");
//                // Apparently process has failed so we can trying again
//                tellTaskImplementation(command);
//            } else {
//                getCaseInstance().addDebugInfo(() -> this + ": skipping definition migration for task implementation - task implementation state indicates it is not yet started. Hence upon start or reactivate the definition will be updated");
//            }
        }

    }

    public TaskImplementationActorState getImplementationState() {
        return implementationState;
    }

}
