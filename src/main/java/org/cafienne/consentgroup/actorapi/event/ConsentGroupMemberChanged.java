package org.cafienne.consentgroup.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.consentgroup.ConsentGroupActor;
import org.cafienne.consentgroup.actorapi.ConsentGroupMember;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Manifest
public class ConsentGroupMemberChanged extends ConsentGroupMemberEvent {
    public final Set<String> rolesRemoved;

    public ConsentGroupMemberChanged(ConsentGroupActor group, ConsentGroupMember newMember, Set<String> rolesRemoved) {
        super(group, newMember);
        this.rolesRemoved = rolesRemoved;
    }

    public ConsentGroupMemberChanged(ValueMap json) {
        super(json);
        rolesRemoved = json.readSet(Fields.rolesRemoved);
    }

    @Override
    public void updateState(ConsentGroupActor group) {
        group.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeConsentGroupMemberEvent(generator);
        writeField(generator, Fields.rolesRemoved, rolesRemoved);
    }
}
