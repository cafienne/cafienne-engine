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

package org.cafienne.processtask.implementation;

import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.cmmn.instance.task.process.ProcessTask;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.definition.InlineSubProcessDefinition;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.definition.SubProcessOutputMappingDefinition;

import java.util.Collection;

/**
 * Process Tasks in the engine can be implemented through this Java interface.
 * The lifecycle of the process task must be triggered by the engine, and subsequently
 * by the implementation of the task.
 * The {@link SubProcessDefinition} is responsible for instantiating sub process implementations.
 * The class implementing this interface must have a constructor that takes the ProcessTask as parameter.
 */
public abstract class InlineSubProcess<T extends InlineSubProcessDefinition> {
    protected final ProcessTask task;
    protected final T definition;

    /**
     * This map contains a typically (fixed) set of variables representing the outcome of the http call. That is: responseCode, responseMessage, output and headers
     */
    private final ValueMap rawOutputParameters = new ValueMap();

    private final ValueMap processOutputParameters = new ValueMap();

    protected InlineSubProcess(ProcessTask processTask, T processDefinition) {
        this.task = processTask;
        this.definition = processDefinition;
    }

    protected final void raiseComplete() {
        transformRawParametersToProcessOutputParameters(definition.getSuccessMappings());
        task.goComplete(processOutputParameters);
    }

    protected final void raiseFault(String description) {
        transformRawParametersToProcessOutputParameters(definition.getFailureMappings());
        task.goFault(processOutputParameters);
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

    protected void addDebugInfo(DebugInfoAppender appender) {
        task.getCaseInstance().addDebugInfo(appender);
    }

    protected void transformRawParametersToProcessOutputParameters(Collection<SubProcessOutputMappingDefinition> mappings) {
        addDebugInfo(() -> "Found " + mappings.size() +" output parameter mappings");
        // No support for raw output mappings currently
        rawOutputParameters.getValue().forEach((name, value) -> {
            processOutputParameters.put(name, value.cloneValueNode());
        });
//        for (SubProcessMapping mapping : mappings) {
//            String outputParameterName = mapping.getTarget().getName();
//            addDebugInfo(() -> "Mapping " + mapping.getSource().getName() +" to " + mapping.getTarget().getName());
//            Value<?> outputParameterValue = mapping.transformOutput(task, rawOutputParameters);
//            setProcessOutputParameter(outputParameterName, outputParameterValue);
//        }
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

    public void migrateDefinition(InlineSubProcessDefinition newDefinition) {
        addDebugInfo(() -> "Migration ProcessDefinition for this type of inline process is not implemented for task " + task + ".");
    }
}
