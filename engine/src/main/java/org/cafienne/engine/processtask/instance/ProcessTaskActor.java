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

package org.cafienne.engine.processtask.instance;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.communication.request.response.ActorRequestFailure;
import org.cafienne.actormodel.communication.request.state.RemoteActorState;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.engine.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.engine.cmmn.actorapi.command.plan.task.FailTask;
import org.cafienne.json.ValueMap;
import org.cafienne.engine.processtask.actorapi.command.*;
import org.cafienne.engine.processtask.actorapi.event.*;
import org.cafienne.engine.processtask.definition.ProcessDefinition;
import org.cafienne.engine.processtask.implementation.SubProcess;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessTaskActor extends ModelActor {

    private final static Logger logger = LoggerFactory.getLogger(ProcessTaskActor.class);
    private ProcessDefinition definition;
    private String name;
    private String parentActorId;
    private String rootActorId;
    private SubProcess<?> taskImplementation;
    private ValueMap inputParameters;
    private ValueMap outputParameters;
    private ParentProcessTaskState processTaskState; // Can only be created when the parent invokes us

    public ProcessTaskActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof ProcessCommand;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return msg instanceof ProcessEvent;
    }

    public ProcessDefinition getDefinition() {
        return definition;
    }

    private void setDefinition(ProcessDefinition definition) {
        this.definition = definition;
    }

    @Override
    public String getParentActorId() {
        return parentActorId;
    }

    @Override
    public String getRootActorId() {
        return rootActorId;
    }

    public ValueMap getMappedInputParameters() {
        return inputParameters;
    }

    public String getName() {
        return name;
    }

    public <S extends SubProcess<?>> S getImplementation() {
        return (S) taskImplementation;
    }

    public void handleStartProcessCommand(StartProcess command) {
//        this.processTaskState = new ParentProcessTaskState(this);
        this.addEvent(new ProcessStarted(this, command));
    }

    public void updateState(ProcessStarted event) {
        this.parentActorId = event.parentActorId;
        this.processTaskState = new ParentProcessTaskState(this);
        this.setEngineVersion(event.engineVersion);
        this.setDebugMode(event.debugMode);
        this.definition = event.definition;
        this.taskImplementation = definition.getImplementation().createInstance(this);
        this.name = event.name;
        this.parentActorId = event.parentActorId;
        this.rootActorId = event.rootActorId;
        this.inputParameters = event.inputParameters;
        if (!recoveryRunning()) {
            addDebugInfo(() -> "Starting process task " + name + " with input: ", inputParameters);
            getImplementation().start();
        }
    }

    public void updateState(ProcessReactivated event) {
        this.taskImplementation = definition.getImplementation().createInstance(this);
        this.inputParameters = event.inputParameters;
        if (!recoveryRunning()) {
            addDebugInfo(() -> "Reactivating process " + getName());
            getImplementation().resetOutput();
            getImplementation().reactivate();
        }
    }

    public void updateState(ProcessSuspended event) {
        if (!recoveryRunning()) {
            addDebugInfo(() -> "Suspending process " + getName());
            getImplementation().suspend();
        }
    }

    public void updateState(ProcessResumed event) {
        if (!recoveryRunning()) {
            addDebugInfo(() -> "Resuming process " + getName());
            getImplementation().resume();
        }
    }

    public void updateState(ProcessTerminated event) {
        if (!recoveryRunning()) {
            addDebugInfo(() -> "Terminating process " + getName());
            getImplementation().terminate();
        }
    }

    public void updateState(ProcessCompleted event) {
        this.outputParameters = event.output;
        addDebugInfo(() -> "Completing process task " + name + " of process type " + getImplementation().getClass().getName() + " with output:", outputParameters);
        if (recoveryFinished()) {
            processTaskState.inform(new CompleteTask(this, outputParameters));
        }
    }

    public void updateState(ProcessFailed event) {
        outputParameters = event.output;
        processTaskState.inform(new FailTask(this, outputParameters));

//                , failure -> {
//            logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
//        }, success -> {
//            addDebugInfo(() -> "Reporting failure of process task " + getId() + " " + name + " in parent was accepted");
//        });
    }

    public void updateState(ProcessDefinitionMigrated event) {
        addDebugInfo(() -> "====== Migrating ProcessTask[" + getId() + "] with name " + getDefinition().getName() + " to a new definition with name " + event.getNewDefinition().getName() + "\n");
        setDefinition(event.getNewDefinition());
        getImplementation().migrateDefinition(event.getNewDefinition().getImplementation());
        addDebugInfo(() -> "====== Completed Migration on ProcessTask[" + getId() + "] with name " + getDefinition().getName());
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    public void completed(ValueMap processOutputParameters) {
        addEvent(new ProcessCompleted(this, processOutputParameters));
    }

    public void failed(String errorDescription, ValueMap processOutputParameters) {
        addDebugInfo(() -> "Encountered failure in process task '" + name + "' of process type " + getImplementation().getClass().getName());
        addDebugInfo(() -> "Error: " + errorDescription);
        addDebugInfo(() -> "Output: ", processOutputParameters);
        addEvent(new ProcessFailed(this, processOutputParameters));
    }

    public void failed(ValueMap processOutputParameters) {
        addDebugInfo(() -> "Reporting failure in process task " + name + " of process type " + getImplementation().getClass().getName() + " with output: ", processOutputParameters);
        addEvent(new ProcessFailed(this, processOutputParameters));
    }

    @Override
    protected void addCommitEvent(IncomingActorMessage message) {
        addEvent(new ProcessModified(this, message));
    }


    private static class ParentProcessTaskState extends RemoteActorState<ProcessTaskActor> {

        public ParentProcessTaskState(ProcessTaskActor actor) {
            super(actor, actor.getParentActorId());
        }

        private void inform(ModelCommand command) {
            if (targetActorId.isEmpty()) {
                // No need to inform about our transitions.
                return;
            }
            sendRequest(command);
//            failure -> {
//                actor.addDebugInfo(() -> "Could not complete process task " + getId() + " " + name + " in parent, due to:", failure.toJson());
//                logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
//            },
//                    success -> addDebugInfo(() -> "Completed process task " + getId() + " '" + name + "' in parent " + parentActorId));
        }

        @Override
        public void handleFailure(ActorRequestFailure failure) {
            actor.addDebugInfo(() -> "Could not complete process task " + actor.getId() + " " + actor.name + " in parent, due to:", failure.toJson());
            logger.error("Could not complete process task " + actor.getId() + " " + actor.name + " in parent, due to:\n" + failure);
        }
    }
}
