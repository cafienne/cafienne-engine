package org.cafienne.cmmn.actorapi.event.team.deprecated.member;

import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a role is removed from a case team member
 */
@Manifest
public class TeamRoleCleared extends CaseTeamRoleEvent {
    public TeamRoleCleared(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        if (isMemberItself()) {
            // The event that removes the member to the team; in practice this one cannot be invoked
            //  since that can only be done through removing the member explicitly, resulting in TeamMemberRemoved event
            return getClass().getSimpleName() + "[" + getMemberDescription()+" is removed from the case team]";
        } else {
            return getClass().getSimpleName() + "[" + getMemberDescription()+" no longer has role " + roleName() + " in the case team]";
        }
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
