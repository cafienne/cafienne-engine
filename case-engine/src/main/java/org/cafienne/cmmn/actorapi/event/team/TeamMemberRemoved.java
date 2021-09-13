package org.cafienne.cmmn.actorapi.event.team;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a member is removed from the case team.
 */
@Manifest
public class TeamMemberRemoved extends DeprecatedCaseTeamEvent {
    public TeamMemberRemoved(Case caseInstance, Member member) {
        super(caseInstance, member);
    }

    public TeamMemberRemoved(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }
}
