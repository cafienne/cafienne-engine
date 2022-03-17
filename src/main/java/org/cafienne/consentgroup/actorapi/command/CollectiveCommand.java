package org.cafienne.consentgroup.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.ConsentGroupUser;
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

abstract class CollectiveCommand extends ConsentGroupCommand {
    protected final ConsentGroup newGroupInfo;

    protected CollectiveCommand(ConsentGroupUser user, ConsentGroup newGroupInfo) {
        super(user, newGroupInfo.id());
        this.newGroupInfo = newGroupInfo;
        validateMemberList();
    }

    protected CollectiveCommand(ValueMap json) {
        super(json);
        this.newGroupInfo = ConsentGroup.deserialize(json.with(Fields.group));
    }

    protected void validateMemberList() {
        if (newGroupInfo.members().isEmpty()) {
            throw new ConsentGroupException("Consent group must have members");
        }
        if (newGroupInfo.members().filter(ConsentGroupMember::isOwner).isEmpty()) {
            throw new ConsentGroupException("Consent group must have at least one owner");
        }
        if (newGroupInfo.members().map(ConsentGroupMember::userId).toSet().size() < newGroupInfo.members().size()) {
            throw new ConsentGroupException("Consent group cannot have duplicate user ids");
        }
    }

    public Seq<ConsentGroupMember> getMembers() {
        return newGroupInfo.members();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.group, this.newGroupInfo);
    }
}
