package org.cafienne.processtask.instance;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.cmmn.akka.command.task.CompleteTask;
import org.cafienne.cmmn.akka.command.task.FailTask;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.ProcessCommand;
import org.cafienne.processtask.akka.event.*;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ProcessTaskActor extends ModelActor<ProcessCommand, ProcessInstanceEvent> {

    private final static Logger logger = LoggerFactory.getLogger(ProcessTaskActor.class);
    private ProcessDefinition definition;
    private String name;
    private String parentActorId;
    private String rootActorId;
    private SubProcess<?> taskImplementation;
    private ValueMap inputParameters;

    public ProcessTaskActor() {
        super(ProcessCommand.class, ProcessInstanceEvent.class);
    }

    @Override
    public ProcessModified createLastModifiedEvent(Instant lastModified) {
        return new ProcessModified(this, lastModified);
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

    public void setInitialState(ProcessStarted event) {
        this.setEngineVersion(event.engineVersion);
        this.debugMode = event.debugMode;
        this.definition = event.definition;
        this.taskImplementation = definition.getImplementation().createInstance(this);
        this.name = event.name;
        this.parentActorId = event.parentActorId;
        this.rootActorId = event.rootActorId;
        this.inputParameters = event.inputParameters;
    }

    public void start() {
        addDebugInfo(() -> "Starting process task " + name + " with input parameters", inputParameters);
        taskImplementation.start();
    }

    public Logger getLogger() {
        return logger;
    }

    public void completed(ValueMap processOutputParameters) {
        addDebugInfo(() -> "Completing process task " + name + " of process type " + taskImplementation.getClass().getName() + "\nOutput:", processOutputParameters);

        addEvent(new ProcessCompleted(this, processOutputParameters));

        askCase(new CompleteTask(this, processOutputParameters), failure -> {
            logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
        }, success -> {
            addDebugInfo(() -> "Completed process task " + getId() + " " + name + " in parent");
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

    public void terminate() {
        addDebugInfo(() -> "Terminating process " + getName());
        taskImplementation.terminate();
        addDebugInfo(() -> "Terminated process implementation");
        addEvent(new ProcessTerminated(this));
    }
}
