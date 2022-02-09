package org.cafienne.cmmn.actorapi.command.team.setmember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to add a user to the case team
 */
@Manifest
public class SetCaseTeamUser extends SetCaseTeamMemberCommand<CaseTeamUser> {
    public SetCaseTeamUser(CaseUserIdentity user, String caseInstanceId, CaseTeamUser newMember) {
        super(user, caseInstanceId, newMember);
    }

    public SetCaseTeamUser(ValueMap json) {
        super(json, CaseTeamUser::deserialize);
    }

    @Override
    protected void process(Team team) {
        team.setUser(newMember);
    }
}
