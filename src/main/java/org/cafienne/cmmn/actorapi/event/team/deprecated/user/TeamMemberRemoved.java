package org.cafienne.cmmn.actorapi.event.team.deprecated.user;

import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a member is removed from the case team.
 */
@Manifest
public class TeamMemberRemoved extends DeprecatedCaseTeamUserEvent {
    public TeamMemberRemoved(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
