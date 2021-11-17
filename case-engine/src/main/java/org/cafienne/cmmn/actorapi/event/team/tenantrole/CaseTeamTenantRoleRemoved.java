package org.cafienne.cmmn.actorapi.event.team.tenantrole;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberRemoved;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamTenantRoleRemoved extends CaseTeamMemberRemoved<CaseTeamTenantRole> {
    public CaseTeamTenantRoleRemoved(Team team, CaseTeamTenantRole caseTeamTenantRole) {
        super(team, caseTeamTenantRole);
    }

    public CaseTeamTenantRoleRemoved(ValueMap json) {
        super(json, CaseTeamTenantRole::deserialize);
    }
}