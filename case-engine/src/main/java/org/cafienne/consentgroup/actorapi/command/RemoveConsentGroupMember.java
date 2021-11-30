package org.cafienne.consentgroup.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class RemoveConsentGroupMember extends ConsentGroupCommand {
    public final String userId;

    public RemoveConsentGroupMember(TenantUser groupOwner, String groupId, String userId) {
        super(groupOwner, groupId);
        this.userId = userId;
    }

    public RemoveConsentGroupMember(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
    }

    @Override
    public void validate(ConsentGroupActor group) throws InvalidCommandException {
        super.validate(group);
        validateNotLastMember(group, userId);
        validateNotLastOwner(group, userId);
    }

    @Override
    public ConsentGroupResponse process(ConsentGroupActor group) {
        group.removeMember(userId);
        return new ConsentGroupResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}