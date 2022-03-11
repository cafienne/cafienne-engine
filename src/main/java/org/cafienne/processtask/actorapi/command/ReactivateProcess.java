package org.cafienne.processtask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessReactivated;
import org.cafienne.processtask.actorapi.event.ProcessStarted;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ReactivateProcess extends ProcessCommand {
    private final ValueMap inputParameters;

    public ReactivateProcess(UserIdentity user, String id, ValueMap inputParameters) {
        super(user, id);
        this.inputParameters = inputParameters;
    }

    public ReactivateProcess(ValueMap json) {
        super(json);
        this.inputParameters = json.readMap(Fields.inputParameters);
    }

    public ValueMap getInputParameters() {
        return inputParameters;
    }

    @Override
    protected void process(ProcessTaskActor processTaskActor, SubProcess<?> implementation) {
        processTaskActor.addEvent(new ProcessReactivated(processTaskActor, this));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.inputParameters, inputParameters);
    }
}
