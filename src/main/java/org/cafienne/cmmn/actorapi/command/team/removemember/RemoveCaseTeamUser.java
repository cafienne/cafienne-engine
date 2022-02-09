package org.cafienne.cmmn.actorapi.command.team.removemember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
import org.cafienne.cmmn.instance.team.MemberType;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to remove a user from the case team
 */
@Manifest
public class RemoveCaseTeamUser extends RemoveCaseTeamMemberCommand<CaseTeamUser> {
    public RemoveCaseTeamUser(CaseUserIdentity user, String caseInstanceId, String userId) {
        super(user, caseInstanceId, userId);
    }

    public RemoveCaseTeamUser(ValueMap json) {
        super(json);
    }

    @Override
    protected MemberType type() {
        return MemberType.TenantRole;
    }

    @Override
    protected CaseTeamUser member(Team team) {
        return team.getUser(memberId);
    }

    @Override
    protected void process(Team team) {
        team.removeUser(memberId);
    }
}
