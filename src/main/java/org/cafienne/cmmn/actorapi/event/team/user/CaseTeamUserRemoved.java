package org.cafienne.cmmn.actorapi.event.team.user;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberRemoved;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group access to the case is removed.
 */
@Manifest
public class CaseTeamUserRemoved extends CaseTeamMemberRemoved<CaseTeamUser> {
    public CaseTeamUserRemoved(Team team, CaseTeamUser user) {
        super(team, user);
    }

    public CaseTeamUserRemoved(ValueMap json) {
        super(json, CaseTeamUser::deserialize);
    }
}
