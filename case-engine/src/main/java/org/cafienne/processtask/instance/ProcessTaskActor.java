package org.cafienne.processtask.instance;

import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.cmmn.akka.command.task.CompleteTask;
import org.cafienne.cmmn.akka.command.task.FailTask;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.processtask.akka.command.ProcessCommand;
import org.cafienne.processtask.akka.command.StartProcess;
import org.cafienne.processtask.akka.event.*;
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

    public ProcessTaskActor() {
        super(ProcessCommand.class, ProcessInstanceEvent.class);
    }

    @Override
    protected CommandHandler createCommandHandler(ProcessCommand msg) {
        return new ProcessTaskCommandHandler(this, msg);
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

    public void start(StartProcess command) {
        this.debugMode = command.debugMode();
        this.definition = command.getDefinition();
        this.taskImplementation = definition.getImplementation().createInstance(this);
        this.name = command.getName();
        this.parentActorId = command.getParentActorId();
        this.rootActorId = command.getRootActorId();
        this.inputParameters = command.getInputParameters();
        currentHandler().addEvent(new ProcessStarted(this, inputParameters));
        addDebugInfo(DebugEvent.class, e -> e.addMessage("Starting process task " + name + " with input parameters", inputParameters));
        taskImplementation.start();
    }

    public Logger getLogger() {
        return logger;
    }

    public void completed(ValueMap processOutputParameters) {
        addDebugInfo(DebugEvent.class, e ->
            e.addMessage("Completing process task " + name + " of process type " + taskImplementation.getClass().getName() + "\nOutput:", processOutputParameters));

        currentHandler().addEvent(new ProcessCompleted(this, processOutputParameters));

        askCase(new CompleteTask(this, processOutputParameters), failure -> {
            logger.error("Could not complete process task " + getId() + " " + name + " in parent, due to:\n" + failure);
        }, success -> {
            addDebugInfo(() -> "Completed process task " + getId() + " " + name + " in parent");
        });
    }

    public void failed(ValueMap processOutputParameters) {
        currentHandler().addEvent(new ProcessFailed(this, processOutputParameters));
        addDebugInfo(DebugEvent.class, e ->
            e.addMessage("Reporting failure in process task " + name + " of process type " + taskImplementation.getClass().getName() + "\nOutput:", processOutputParameters));

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
        currentHandler().addEvent(new ProcessTerminated(this));
    }
}
