package org.cafienne.processtask.actorapi.command.response;

import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
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
