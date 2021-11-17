package org.cafienne.cmmn.actorapi.command.team.setmember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to add a tenant role to the case team
 */
@Manifest
public class SetCaseTeamTenantRole extends SetCaseTeamMemberCommand<CaseTeamTenantRole> {
    public SetCaseTeamTenantRole(CaseUserIdentity user, String caseInstanceId, CaseTeamTenantRole newMember) {
        super(user, caseInstanceId, newMember);
    }

    public SetCaseTeamTenantRole(ValueMap json) {
        super(json, CaseTeamTenantRole::deserialize);
    }

    @Override
    protected void process(Team team) {
        team.setTenantRole(newMember);
    }
}
