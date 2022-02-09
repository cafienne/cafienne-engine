package org.cafienne.cmmn.actorapi.event.team.deprecated.member;

import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a role is added to a case team member
 */
@Manifest
public class TeamRoleFilled extends CaseTeamRoleEvent {
    public TeamRoleFilled(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        if (isMemberItself()) {
            // The event that adds the member to the team
            return getClass().getSimpleName() + "[" + getMemberDescription() + " is added to the case team]";
        } else {
            return getClass().getSimpleName() + "[" + getMemberDescription() + " now has role " + roleName() + " in the case team]";
        }
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
