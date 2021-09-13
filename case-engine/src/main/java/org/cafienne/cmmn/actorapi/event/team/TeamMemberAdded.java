package org.cafienne.cmmn.actorapi.event.team;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a userId is added to the case team.
 */
@Manifest
public class TeamMemberAdded extends DeprecatedCaseTeamEvent {
    public TeamMemberAdded(Case caseInstance, Member member) {
        super(caseInstance, member);
        throw new IllegalArgumentException("This API is no longer supported; only for backwards compatibility");
    }

    public TeamMemberAdded(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }
}
