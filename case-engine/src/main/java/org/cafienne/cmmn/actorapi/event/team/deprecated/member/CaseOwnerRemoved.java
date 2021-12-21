package org.cafienne.cmmn.actorapi.event.team.deprecated.member;

import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a team member is no longer owner
 */
@Manifest
public class CaseOwnerRemoved extends DeprecatedCaseTeamEvent {
    public CaseOwnerRemoved(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getMemberDescription()+" is no longer owner]";
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }
}
