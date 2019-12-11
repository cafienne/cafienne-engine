package org.cafienne.processtask.akka.command.response;

import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.akka.command.ProcessCommand;

@Manifest
public class ProcessResponse extends ModelResponse {
    public ProcessResponse(ProcessCommand command) {
        super(command);
    }

    public ProcessResponse(ValueMap json) {
        super(json);
    }
}
