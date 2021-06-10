package org.cafienne.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.cmmn.instance.Case;

import java.io.IOException;

@Manifest
public class CaseAppliedPlatformUpdate extends CaseEvent {
    public final PlatformUpdate newUserInformation;

    public CaseAppliedPlatformUpdate(Case tenant, PlatformUpdate newUserInformation) {
        super(tenant);
        this.newUserInformation = newUserInformation;
    }

    public CaseAppliedPlatformUpdate(ValueMap json) {
        super(json);
        newUserInformation = PlatformUpdate.deserialize(json.withArray(Fields.users));
    }

    @Override
    public void updateState(Case actor) {
        actor.updateState(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        writeField(generator, Fields.users, newUserInformation.toValue());
    }
}
