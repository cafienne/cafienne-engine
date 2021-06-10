package org.cafienne.cmmn.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.actormodel.event.TransactionEvent;
import org.cafienne.actormodel.identity.PlatformUser;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.platform.CaseUpdate;
import org.cafienne.cmmn.actorapi.command.platform.PlatformUpdate;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;

import java.io.IOException;

@Manifest
public class UpdateCaseWithPlatformInformation extends CaseCommand {
    private final PlatformUpdate newUserInformation;

    public UpdateCaseWithPlatformInformation(PlatformUser user, CaseUpdate action) {
        super(TenantUser.fromPlatformOwner(user, action.tenant()), action.caseId());
        this.newUserInformation = action.platformUpdate();
    }

    public UpdateCaseWithPlatformInformation(ValueMap json) {
        super(json);
        newUserInformation = PlatformUpdate.deserialize(json.withArray(Fields.users));
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
    }

    @Override
    public TransactionEvent createTransactionEvent(Case actor) {
        return null;
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        caseInstance.updatePlatformInformation(this.newUserInformation);
        return new CaseResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.users, newUserInformation.toValue());
    }
}

