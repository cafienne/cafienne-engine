package org.cafienne.cmmn.actorapi.event.team.deprecated.user;

import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a userId is added to the case team.
 */
@Manifest
public class TeamMemberAdded extends DeprecatedCaseTeamUserEvent {
    public TeamMemberAdded(ValueMap json) {
        super(json);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
