package org.cafienne.consentgroup.actorapi.response;

import org.cafienne.actormodel.response.BaseModelResponse;
import org.cafienne.consentgroup.actorapi.ConsentGroupMessage;
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ConsentGroupResponse extends BaseModelResponse implements ConsentGroupMessage {
    public ConsentGroupResponse(ConsentGroupCommand command) {
        super(command);
    }

    public ConsentGroupResponse(ValueMap json) {
        super(json);
    }
}
