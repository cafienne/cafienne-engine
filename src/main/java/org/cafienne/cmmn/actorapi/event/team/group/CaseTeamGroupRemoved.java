package org.cafienne.cmmn.actorapi.event.team.group;

import org.cafienne.cmmn.actorapi.command.team.CaseTeamGroup;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberRemoved;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a consent group access to the case is removed.
 */
@Manifest
public class CaseTeamGroupRemoved extends CaseTeamMemberRemoved<CaseTeamGroup> {
    public CaseTeamGroupRemoved(Team team, CaseTeamGroup group) {
        super(team, group);
    }

    public CaseTeamGroupRemoved(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
    }
}
