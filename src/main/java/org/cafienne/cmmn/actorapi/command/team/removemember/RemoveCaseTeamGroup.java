package org.cafienne.cmmn.actorapi.command.team.removemember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamGroup;
import org.cafienne.cmmn.instance.team.MemberType;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to remove a consent group from the case team
 */
@Manifest
public class RemoveCaseTeamGroup extends RemoveCaseTeamMemberCommand<CaseTeamGroup> {
    public RemoveCaseTeamGroup(CaseUserIdentity user, String caseInstanceId, String groupId) {
        super(user, caseInstanceId, groupId);
    }

    public RemoveCaseTeamGroup(ValueMap json) {
        super(json);
    }

    @Override
    protected MemberType type() {
        return MemberType.TenantRole;
    }

    @Override
    protected CaseTeamGroup member(Team team) {
        return team.getGroup(memberId);
    }

    @Override
    protected void process(Team team) {
        team.removeGroup(memberId);
    }
}
