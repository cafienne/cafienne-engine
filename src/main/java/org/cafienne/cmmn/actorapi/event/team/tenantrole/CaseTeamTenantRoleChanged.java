package org.cafienne.cmmn.actorapi.event.team.tenantrole;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamTenantRole;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberChanged;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamTenantRoleChanged extends CaseTeamMemberChanged<CaseTeamTenantRole> {
    public CaseTeamTenantRoleChanged(Team team, CaseTeamTenantRole newInfo) throws CaseTeamError {
        super(team, newInfo);
    }

    public CaseTeamTenantRoleChanged(ValueMap json) {
        super(json, CaseTeamTenantRole.getDeserializer(json));
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
