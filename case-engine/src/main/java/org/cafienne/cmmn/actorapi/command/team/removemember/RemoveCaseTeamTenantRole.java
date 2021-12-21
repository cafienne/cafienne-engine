package org.cafienne.cmmn.actorapi.command.team.removemember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.instance.team.MemberType;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to remove a tenant role from the case team
 */
@Manifest
public class RemoveCaseTeamTenantRole extends RemoveCaseTeamMemberCommand<CaseTeamTenantRole> {
    public RemoveCaseTeamTenantRole(CaseUserIdentity user, String caseInstanceId, String tenantRoleName) {
        super(user, caseInstanceId, tenantRoleName);
    }

    public RemoveCaseTeamTenantRole(ValueMap json) {
        super(json);
    }

    @Override
    protected MemberType type() {
        return MemberType.TenantRole;
    }

    @Override
    protected CaseTeamTenantRole member(Team team) {
        return team.getTenantRole(memberId);
    }

    @Override
    protected void process(Team team) {
        team.removeTenantRole(memberId);
    }
}
