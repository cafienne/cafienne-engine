package org.cafienne.cmmn.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.ModelActor;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.identity.PlatformUser;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.platform.CaseUpdate;
import org.cafienne.cmmn.akka.command.platform.PlatformUpdate;
import org.cafienne.cmmn.akka.command.platform.TenantUpdate;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.tenant.TenantActor;
import org.cafienne.tenant.akka.command.platform.PlatformTenantCommand;
import org.cafienne.tenant.akka.command.response.TenantResponse;

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

