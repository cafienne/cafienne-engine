package org.cafienne.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.ReactivateProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ProcessReactivated extends BaseProcessEvent {
    public final ValueMap inputParameters;

    public ProcessReactivated(ProcessTaskActor actor, ReactivateProcess command) {
        super(actor);
        this.inputParameters = command.getInputParameters();
    }

    public ProcessReactivated(ValueMap json) {
        super(json);
        this.inputParameters = json.readMap(Fields.input);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.input, inputParameters);
    }}
