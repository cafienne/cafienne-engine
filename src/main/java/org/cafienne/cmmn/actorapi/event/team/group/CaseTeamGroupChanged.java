package org.cafienne.cmmn.actorapi.event.team.group;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamGroup;
import org.cafienne.cmmn.actorapi.command.team.GroupRoleMapping;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberChanged;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamGroupChanged extends CaseTeamMemberChanged<CaseTeamGroup> {
    public CaseTeamGroupChanged(Team team, CaseTeamGroup newInfo) throws CaseTeamError {
        super(team, newInfo);
    }

    public CaseTeamGroupChanged(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamMemberEvent(generator);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
