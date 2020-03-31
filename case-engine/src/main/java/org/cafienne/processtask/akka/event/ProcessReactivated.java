package org.cafienne.processtask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.ReactivateProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ProcessReactivated extends ProcessInstanceEvent {
    public final ValueMap inputParameters;

    private enum Fields {
        input
    }

    public ProcessReactivated(ProcessTaskActor actor, ReactivateProcess command) {
        super(actor);
        this.inputParameters = command.getInputParameters();
    }

    public ProcessReactivated(ValueMap json) {
        super(json);
        this.inputParameters = readMap(json, Fields.input);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }

    @Override
    public void runImmediateBehavior() {
        actor.reactivate();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.input, inputParameters);
    }}
