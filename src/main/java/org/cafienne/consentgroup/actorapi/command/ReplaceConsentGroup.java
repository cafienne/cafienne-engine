package org.cafienne.consentgroup.actorapi.command;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroup;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class ReplaceConsentGroup extends CollectiveCommand {
    public ReplaceConsentGroup(TenantUser tenantOwner, ConsentGroup newGroupInfo) {
        super(tenantOwner, newGroupInfo);
    }

    public ReplaceConsentGroup(ValueMap json) {
        super(json);
    }

    public boolean missingUserId(String userId) {
        return newGroupInfo.members().find(member -> member.userId().equals(userId)).isEmpty();
    }

    @Override
    public ConsentGroupResponse process(ConsentGroupActor group) {
        group.replace(this);
        return new ConsentGroupResponse(this);
    }
}
