package org.cafienne.cmmn.actorapi.event.team.user;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberChanged;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamUserChanged extends CaseTeamMemberChanged<CaseTeamUser> {
    public CaseTeamUserChanged(Team team, CaseTeamUser newInfo) throws CaseTeamError {
        super(team, newInfo);
    }

    public CaseTeamUserChanged(ValueMap json) {
        super(json, CaseTeamUser.getDeserializer(json));
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
