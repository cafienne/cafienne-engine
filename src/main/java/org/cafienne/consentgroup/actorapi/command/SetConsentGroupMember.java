package org.cafienne.consentgroup.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.ConsentGroupUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroupMember;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class SetConsentGroupMember extends ConsentGroupCommand {
    private final ConsentGroupMember newMemberInfo;

    public SetConsentGroupMember(ConsentGroupUser groupOwner, ConsentGroupMember newMemberInfo) {
        super(groupOwner, groupOwner.groupId());
        this.newMemberInfo = newMemberInfo;
    }

    public SetConsentGroupMember(ValueMap json) {
        super(json);
        this.newMemberInfo = ConsentGroupMember.deserialize(json.with(Fields.member));
    }

    @Override
    public void validate(ConsentGroupActor group) throws InvalidCommandException {
        super.validate(group);
        // Check that new member is not last owner
        if (! newMemberInfo.isOwner()) {
            // ... then check this member is not the last owner.
            validateNotLastOwner(group, newMemberInfo.userId());
        }
    }

    @Override
    public ConsentGroupResponse process(ConsentGroupActor group) {
        group.setMember(newMemberInfo);
        return new ConsentGroupResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.member, newMemberInfo.toValue());
    }
}