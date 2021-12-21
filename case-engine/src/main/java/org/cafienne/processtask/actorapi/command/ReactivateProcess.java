package org.cafienne.processtask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
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
    public ProcessResponse process(ProcessTaskActor process) {
        return process.reactivate(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.inputParameters, inputParameters);
    }
}
