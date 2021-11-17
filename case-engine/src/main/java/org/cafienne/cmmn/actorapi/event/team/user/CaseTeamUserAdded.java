package org.cafienne.cmmn.actorapi.event.team.user;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberEvent;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamUserAdded extends CaseTeamMemberEvent<CaseTeamUser> {
    public CaseTeamUserAdded(Team team, CaseTeamUser newInfo) {
        super(team, newInfo);
    }

    public CaseTeamUserAdded(ValueMap json) {
        super(json, CaseTeamUser::deserialize);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
