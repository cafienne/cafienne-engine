package org.cafienne.cmmn.actorapi.event.team.tenantrole;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberEvent;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamTenantRoleAdded extends CaseTeamMemberEvent<CaseTeamTenantRole> {
    public CaseTeamTenantRoleAdded(Team team, CaseTeamTenantRole newInfo) {
        super(team, newInfo);
    }

    public CaseTeamTenantRoleAdded(ValueMap json) {
        super(json, CaseTeamTenantRole::deserialize);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
