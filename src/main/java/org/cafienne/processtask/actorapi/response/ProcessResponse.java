package org.cafienne.processtask.actorapi.response;

import org.cafienne.actormodel.response.BaseModelResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.ProcessActorMessage;
import org.cafienne.processtask.actorapi.command.ProcessCommand;

@Manifest
public class ProcessResponse extends BaseModelResponse implements ProcessActorMessage {
    public ProcessResponse(ProcessCommand command) {
        super(command);
    }

    public ProcessResponse(ValueMap json) {
        super(json);
    }
}
