package org.cafienne.cmmn.actorapi.event.team.user;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
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
public class CaseTeamUserChanged extends CaseTeamMemberChanged<CaseTeamUser> {
    public CaseTeamUserChanged(Team team, CaseTeamUser newInfo, Set<String> rolesRemoved) throws CaseTeamError {
        super(team, newInfo, rolesRemoved);
    }

    public CaseTeamUserChanged(ValueMap json) {
        super(json, CaseTeamUser::deserialize);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
