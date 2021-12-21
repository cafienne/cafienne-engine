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
import java.util.Set;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public class CaseTeamGroupChanged extends CaseTeamMemberChanged<CaseTeamGroup> {
    public final Collection<GroupRoleMapping> removedMappings;

    private static Set<String> removedRoles(Set<GroupRoleMapping> mappingsRemoved) {
        Set<String> caseRoles = new HashSet<>();
        mappingsRemoved.forEach((GroupRoleMapping m) -> caseRoles.addAll(m.getCaseRoles()));
        return caseRoles;
    }

    public CaseTeamGroupChanged(Team team, CaseTeamGroup newMemberInfo, Set<GroupRoleMapping> mappingsRemoved) throws CaseTeamError {
        super(team, newMemberInfo, removedRoles(mappingsRemoved));
        this.removedMappings = mappingsRemoved;
    }

    public CaseTeamGroupChanged(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
        this.removedMappings = json.readObjects(Fields.removedMappings, GroupRoleMapping::deserialize);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamMemberEvent(generator);
        writeListField(generator, Fields.removedMappings, removedMappings);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(member);
    }
}
