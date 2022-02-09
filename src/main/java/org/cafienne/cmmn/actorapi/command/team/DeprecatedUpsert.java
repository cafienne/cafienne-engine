package org.cafienne.cmmn.actorapi.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class DeprecatedUpsert extends CaseTeamCommand {
    private final UpsertMemberData memberData;

    public DeprecatedUpsert(CaseUserIdentity user, String caseInstanceId, UpsertMemberData member) {
        super(user, caseInstanceId);
        this.memberData = member;
    }

    public DeprecatedUpsert(ValueMap json) {
        super(json);
        this.memberData = UpsertMemberData.deserialize(json.with(Fields.member));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.member, memberData);
    }

    @Override
    protected void validate(Team team) throws InvalidCommandException {
        memberData.validateRolesExist(team.getDefinition());
        memberData.validateNotLastOwner(team);
    }

    @Override
    protected void process(Team team) {
        team.upsert(memberData);
    }
}
