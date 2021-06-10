package org.cafienne.processtask.instance;

import org.cafienne.actormodel.CaseSystem;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.cmmn.actorapi.command.plan.task.CompleteTask;
import org.cafienne.cmmn.actorapi.command.plan.task.FailTask;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.processtask.actorapi.command.*;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.processtask.actorapi.event.*;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessTaskActor extends ModelActor<ProcessCommand, ProcessInstanceEvent> {

    private final static Logger logger = LoggerFactory.getLogger(ProcessTaskActor.class);
    private ProcessDefinition definition;
    private String name;
    private String parentActorId;
    private String rootActorId;
    private SubProcess<?> taskImplementation;
    private ValueMap inputParameters;

    public ProcessTaskActor(CaseSystem caseSystem) {
        super(ProcessCommand.class, ProcessInstanceEvent.class, caseSystem);
    }

    @Override
    public ProcessModified createTransactionEvent() {
        return new ProcessModified(this, getTransactionTimestamp());
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

    public void updateState(ProcessStarted event) {
        this.setEngineVersion(event.engineVersion);
        this.setDebugMode(event.debugMode);
        this.definition = event.definition;
        this.taskImplementation = definition.getImplementation().createInstance(this);
        this.name = event.name;
        this.parentActorId = event.parentActorId;
        this.rootActorId = event.rootActorId;
        this.inputParameters = event.inputParameters;
    }

    public void updateState(ProcessReactivated event) {
        this.inputParameters = event.inputParameters;
    }

    public ProcessResponse start(StartProcess command) {
        addEvent(new ProcessStarted(this, command));
        addDebugInfo(() -> "Starting process task " + name + " with input parameters", inputParameters);
        taskImplementation.start();
        return new ProcessResponse(command);
    }

    public ProcessResponse reactivate(ReactivateProcess command) {
        addEvent(new ProcessReactivated(this, command));
        addDebugInfo(() -> "Reactivating process " + getName());
        taskImplementation.resetOutput();
        taskImplementation.reactivate();
        return new ProcessResponse(command);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public String getDescription() {
        return "ProcessTask[" + getId() + "]";
    }

    public void completed(ValueMap processOutputParameters) {
        addDebugInfo(() -> "Completing process task " + name + " of process type " + taskImplementation.getClass().getName() + "\nOutput:", processOutputParameters);

        addEvent(new ProcessCompleted(this, processOutputParameters));

        askCase(new CompleteTask(this, processOutputParameters),
            failure -> {
                addDebugInfo(() -> "Could not complete process task " + getId() + " " + name + " in parent, due to:", failure.toJson());
                logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
            },
            success -> addDebugInfo(() -> "Completed process task " + getId() + " '" + name + "' in parent " + parentActorId));
    }

    public void failed(String errorDescription, ValueMap processOutputParameters) {
        addEvent(new ProcessFailed(this, processOutputParameters));
        addDebugInfo(() -> "Encountered failure in process task '" + name + "' of process type " + taskImplementation.getClass().getName());
        addDebugInfo(() -> "Error: " + errorDescription, processOutputParameters);

        askCase(new FailTask(this, processOutputParameters), failure -> {
            logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
        }, success -> {
            addDebugInfo(() -> "Reporting failure of process task " + getId() + " " + name + " in parent was accepted");
        });
    }

    public void failed(ValueMap processOutputParameters) {
        addEvent(new ProcessFailed(this, processOutputParameters));
        addDebugInfo(() -> "Reporting failure in process task " + name + " of process type " + taskImplementation.getClass().getName() + "\nOutput:", processOutputParameters);

        askCase(new FailTask(this, processOutputParameters), failure -> {
            logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
        }, success -> {
            addDebugInfo(() -> "Reporting failure of process task " + getId() + " " + name + " in parent was accepted");
        });
    }

    public ProcessResponse suspend(SuspendProcess command) {
        addEvent(new ProcessSuspended(this));
        addDebugInfo(() -> "Suspending process " + getName());
        taskImplementation.suspend();
        return new ProcessResponse(command);
    }

    public ProcessResponse resume(ResumeProcess command) {
        addEvent(new ProcessResumed(this));
        addDebugInfo(() -> "Resuming process " + getName());
        taskImplementation.resume();
        return new ProcessResponse(command);
    }

    public ProcessResponse terminate(TerminateProcess command) {
        addEvent(new ProcessTerminated(this));
        addDebugInfo(() -> "Terminating process " + getName());
        taskImplementation.terminate();
        return new ProcessResponse(command);
    }
}
