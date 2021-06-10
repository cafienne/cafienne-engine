package org.cafienne.processtask.actorapi.response;

import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.command.ProcessCommand;

@Manifest
public class ProcessResponse extends ModelResponse {
    public ProcessResponse(ProcessCommand command) {
        super(command);
    }

    public ProcessResponse(ValueMap json) {
        super(json);
    }
}
