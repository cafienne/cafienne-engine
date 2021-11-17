package org.cafienne.cmmn.actorapi.event.team.deprecated.member;

import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a team member has become owner
 */
@Manifest
public class CaseOwnerAdded extends DeprecatedCaseTeamEvent {
    public CaseOwnerAdded(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getMemberDescription()+" is now owner]";
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
