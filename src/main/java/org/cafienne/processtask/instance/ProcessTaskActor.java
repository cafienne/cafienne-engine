package org.cafienne.processtask.instance;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.*;
import org.cafienne.processtask.actorapi.event.*;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
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

    public ProcessTaskActor(CaseSystem caseSystem) {
        super(caseSystem);
    }

    @Override
    protected boolean supportsCommand(Object msg) {
        return msg instanceof ProcessCommand;
    }

    @Override
    protected boolean supportsEvent(ModelEvent msg) {
        return msg instanceof ProcessInstanceEvent;
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

    public void updateState(ProcessInstanceEvent event) {

    }

    public void updateState(ProcessStarted event) {
        this.setEngineVersion(event.engineVersion);
        this.setDebugMode(event.debugMode);
        this.definition = event.definition;
        this.taskImplementation = definition.getImplementation().createInstance(this);
        this.name = event.name;
        this.parentActorId = event.parentActorId;
        this.rootActorId = event.rootActorId;
        this.inputParameters = event.inputParameters;
        if (! recoveryRunning()) {
            addDebugInfo(() -> "Starting process task " + name + " with input: ", inputParameters);
            getImplementation().start();
        }
    }

    public void updateState(ProcessReactivated event) {
        this.inputParameters = event.inputParameters;
        if (! recoveryRunning()) {
            addDebugInfo(() -> "Reactivating process " + getName());
            getImplementation().resetOutput();
            getImplementation().reactivate();
        }
    }

    public void updateState(ProcessSuspended event) {
        if (! recoveryRunning()) {
            addDebugInfo(() -> "Suspending process " + getName());
            getImplementation().suspend();
        }
    }

    public void updateState(ProcessResumed event) {
        if (! recoveryRunning()) {
            addDebugInfo(() -> "Resuming process " + getName());
            getImplementation().resume();
        }
    }

    public void updateState(ProcessTerminated event) {
        if (! recoveryRunning()) {
            addDebugInfo(() -> "Terminating process " + getName());
            getImplementation().terminate();
        }
    }

    public void updateState(ProcessCompleted event) {
        this.outputParameters = event.output;
        addDebugInfo(() -> "Completing process task " + name + " of process type " + getImplementation().getClass().getName() + " with output:", outputParameters);
        if (recoveryFinished()) {
            askCase(new CompleteTask(this, outputParameters),
                    failure -> {
                        addDebugInfo(() -> "Could not complete process task " + getId() + " " + name + " in parent, due to:", failure.toJson());
                        logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
                    },
                    success -> addDebugInfo(() -> "Completed process task " + getId() + " '" + name + "' in parent " + parentActorId));
        }
    }

    public void updateState(ProcessFailed event) {
        outputParameters = event.output;
        askCase(new FailTask(this, outputParameters), failure -> {
            logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
        }, success -> {
            addDebugInfo(() -> "Reporting failure of process task " + getId() + " " + name + " in parent was accepted");
        });
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
    protected void completeTransaction(IncomingActorMessage source) {
        addEvent(new ProcessModified(this, source));
    }
}
