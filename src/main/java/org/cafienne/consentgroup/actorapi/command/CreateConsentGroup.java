package org.cafienne.consentgroup.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroup;
import org.cafienne.consentgroup.actorapi.ConsentGroupMember;
import org.cafienne.consentgroup.actorapi.exception.ConsentGroupException;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupCreatedResponse;
import org.cafienne.consentgroup.actorapi.response.ConsentGroupResponse;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import scala.collection.immutable.Seq;

import java.io.IOException;

@Manifest
public class CreateConsentGroup extends ConsentGroupCommand implements BootstrapMessage {
    private final ConsentGroup newGroupInfo;

    public CreateConsentGroup(TenantUser tenantOwner, ConsentGroup newGroupInfo) {
        super(tenantOwner, newGroupInfo.id());
        this.newGroupInfo = newGroupInfo;
    }

    public CreateConsentGroup(ValueMap json) {
        super(json);
        this.newGroupInfo = ConsentGroup.deserialize(json.with(Fields.group));
    }

    public Seq<ConsentGroupMember> getMembers() {
        return newGroupInfo.members();
    }

    @Override
    public void validate(ConsentGroupActor groupActor) throws InvalidCommandException {
        if (groupActor.exists()) {
            throw new ConsentGroupException("Consent group already exists");
        }
        if (newGroupInfo.members().isEmpty()) {
            throw new ConsentGroupException("Consent group must have members");
        }
        if (newGroupInfo.members().filter(ConsentGroupMember::isOwner).isEmpty()) {
            throw new ConsentGroupException("Consent group must have at least one owner");
        }
    }

    @Override
    public ConsentGroupResponse process(ConsentGroupActor group) {
        group.create(this);
        return new ConsentGroupCreatedResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.group, this.newGroupInfo);
    }

    @Override
    public String tenant() {
        return newGroupInfo.tenant();
    }
}
