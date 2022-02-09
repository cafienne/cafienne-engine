package org.cafienne.consentgroup.actorapi.response;

import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

@Manifest
public class ConsentGroupCreatedResponse extends ConsentGroupResponse {
    public ConsentGroupCreatedResponse(ConsentGroupCommand command) {
        super(command);
    }

    public ConsentGroupCreatedResponse(ValueMap json) {
        super(json);
    }

    @Override
    public Value<?> toJson() {
        return new ValueMap(Fields.groupId, getActorId());
    }
}
