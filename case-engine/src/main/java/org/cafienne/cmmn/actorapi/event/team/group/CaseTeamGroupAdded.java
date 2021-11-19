package org.cafienne.cmmn.actorapi.event.team.group;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamGroup;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberEvent;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamGroupAdded extends CaseTeamMemberEvent<CaseTeamGroup> {
    public CaseTeamGroupAdded(Team team, CaseTeamGroup newInfo) throws CaseTeamError {
        super(team, newInfo);
    }
    public CaseTeamGroupAdded(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
