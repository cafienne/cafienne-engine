package org.cafienne.consentgroup.actorapi.response;

import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ConsentGroupResponse extends ModelResponse {
    public ConsentGroupResponse(ConsentGroupCommand command) {
        super(command);
    }

    public ConsentGroupResponse(ValueMap json) {
        super(json);
    }
}
