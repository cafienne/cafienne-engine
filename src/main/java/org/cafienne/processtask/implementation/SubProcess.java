/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation;

import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.definition.SubProcessOutputMappingDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Process Tasks in the engine can be implemented through this Java interface.
 * The lifecycle of the process task must be triggered by the engine, and subsequently
 * by the implementation of the task.
 * The {@link SubProcessDefinition} is responsible for instantiating sub process implementations.
 * The class implementing this interface must have a constructor that takes the ProcessTask as parameter.
 */
public abstract class SubProcess<T extends SubProcessDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(SubProcess.class);
    protected final ProcessTaskActor processTaskActor;
    protected final T definition;

    /**
     * This map contains a typically (fixed) set of variables representing the outcome of the http call. That is: responseCode, responseMessage, output and headers
     */
    private final ValueMap rawOutputParameters = new ValueMap();
    
    private final ValueMap processOutputParameters = new ValueMap();

    protected SubProcess(ProcessTaskActor processTask, T processDefinition) {
        this.processTaskActor = processTask;
        this.definition = processDefinition;
    }

    protected final void raiseComplete() {
        transformRawParametersToProcessOutputParameters(definition.getSuccessMappings());
        processTaskActor.completed(processOutputParameters);
    }

    protected final void raiseFault(String description) {
        transformRawParametersToProcessOutputParameters(definition.getFailureMappings());
        processTaskActor.failed(description, processOutputParameters);
    }

    /**
     * Store an exception in the process output parameters (and in the raw parameters),
     * under the name {@link SubProcessDefinition#EXCEPTION_PARAMETER}, and raise fault for the task.
     * @param cause
     */
    protected void raiseFault(String message, Throwable cause) {
        setFault(Value.convert(cause));
        raiseFault(message);
    }

    /**
     * Stores the value as an output parameter both in the raw and in the process output parameters.
     * This enables propagating faults back into the process task without any in-process mappings defined.
     * @param value
     */
    protected void setFault(Value<?> value) {
        String name = SubProcessDefinition.EXCEPTION_PARAMETER;
        rawOutputParameters.put(name, value);
        processOutputParameters.put(name, value);
    }

    /**
     * Invoked before reactive is invoked. Clears output parameters of earlier failures.
     */
    public void resetOutput() {
        rawOutputParameters.getValue().clear();
        processOutputParameters.getValue().clear();
    }

    protected void transformRawParametersToProcessOutputParameters(Collection<SubProcessOutputMappingDefinition> mappings) {
        processTaskActor.addDebugInfo(() -> "Found " + mappings.size() +" output parameter mappings");
        for (SubProcessOutputMappingDefinition mapping : mappings) {
            String outputParameterName = mapping.getTarget().getName();
            processTaskActor.addDebugInfo(() -> "Mapping " + mapping.getSource().getName() +" to " + mapping.getTarget().getName());
            Value<?> outputParameterValue = mapping.transformOutput(processTaskActor, rawOutputParameters);
            setProcessOutputParameter(outputParameterName, outputParameterValue);
        }
    }

    /**
     * Sets a raw process parameter value; this may serve as input for a mapping
     * into the process output parameters.
     * @param name
     * @param value
     */
    protected void setRawOutputParameter(String name, Value<?> value) {
        rawOutputParameters.put(name, value);
    }

    /**
     * Directly set a process output parameter. Note that the value may be overriden by the mappings
     * that are invoked upon completion or failure of the process.
     * @param name
     * @param value
     */
    protected void setProcessOutputParameter(String name, Value<?> value) {
        processOutputParameters.put(name, value);
    }
    
    /**
     * Contains the map of raw parameter values of this sub process. Upon completion or failure of this instance, these parameters
     * will be mapped into the process output parameters if mappings are defined for them.
     * @return
     */
    protected ValueMap getRawOutputParameters() {
        return rawOutputParameters;
    }
    
    /**
     * Returns the map with the output parameters of the process, which will be used to map back into the ProcessTask parameters.
     * @return
     */
    protected ValueMap getProcessOutputParameters() {
        return processOutputParameters;
    }
    
    /**
     * Start is invoked when the Task has become {@link State#Active}, either through {@link Transition#Start} or {@link Transition#ManualStart}
     */
    public abstract void start();

    /**
     * Reactive is invoked when the Task has become {@link State#Active}, through {@link Transition#Reactivate}
     */
    public abstract void reactivate();

    /**
     * Reactive is invoked when the Task has become {@link State#Suspended}, through {@link Transition#Suspend}
     */
    public abstract void suspend();

    /**
     * Terminate is invoked when the Task has become {@link State#Terminated}, through {@link Transition#Terminate}, {@link Transition#ParentTerminate} or
     * {@link Transition#Exit}
     */
    public abstract void terminate();

    /**
     * Terminate is invoked when the Task has become {@link State#Active}, through {@link Transition#Resume} or {@link Transition#ParentResume}
     */
    public abstract void resume();

}
