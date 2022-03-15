package org.cafienne.consentgroup.actorapi.command;

import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroup;
import org.cafienne.consentgroup.actorapi.exception.ConsentGroupException;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupCreatedResponse;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class CreateConsentGroup extends CollectiveCommand implements BootstrapMessage {
    public CreateConsentGroup(TenantUser tenantOwner, ConsentGroup newGroupInfo) {
        super(tenantOwner, newGroupInfo);
    }

    public CreateConsentGroup(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(ConsentGroupActor groupActor) throws InvalidCommandException {
        if (groupActor.exists()) {
            throw new ConsentGroupException("Consent group already exists");
        }
    }

    @Override
    public ConsentGroupResponse process(ConsentGroupActor group) {
        group.create(this);
        return new ConsentGroupCreatedResponse(this);
    }

    @Override
    public String tenant() {
        return newGroupInfo.tenant();
    }
}
