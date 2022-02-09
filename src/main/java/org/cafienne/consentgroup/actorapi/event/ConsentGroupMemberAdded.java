package org.cafienne.consentgroup.actorapi.event;

import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroupMember;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ConsentGroupMemberAdded extends ConsentGroupMemberEvent {
    public ConsentGroupMemberAdded(ConsentGroupActor group, ConsentGroupMember member) {
        super(group, member);
    }

    public ConsentGroupMemberAdded(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" - " + userId;
    }

    @Override
    public void updateState(ConsentGroupActor group) {
        group.updateState(this);
    }
}
