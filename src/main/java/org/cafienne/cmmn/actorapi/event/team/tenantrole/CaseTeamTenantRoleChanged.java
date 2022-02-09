package org.cafienne.cmmn.actorapi.event.team.tenantrole;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberChanged;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.util.Set;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamTenantRoleChanged extends CaseTeamMemberChanged<CaseTeamTenantRole> {
    public CaseTeamTenantRoleChanged(Team team, CaseTeamTenantRole newMemberInfo, Set<String> removedRoles) throws CaseTeamError {
        super(team, newMemberInfo, removedRoles);
    }

    public CaseTeamTenantRoleChanged(ValueMap json) {
        super(json, CaseTeamTenantRole::deserialize);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
