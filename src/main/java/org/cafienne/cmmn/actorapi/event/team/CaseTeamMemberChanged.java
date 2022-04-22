package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Set;

/**
 * Event caused when a consent group is given access to the case.
 */
@Manifest
public abstract class CaseTeamMemberChanged<Member extends CaseTeamMember> extends CaseTeamMemberEvent<Member> {
    public CaseTeamMemberChanged(Team team, Member newInfo) throws CaseTeamError {
        super(team, newInfo);
    }

    public CaseTeamMemberChanged(ValueMap json, CaseTeamMemberDeserializer<Member> reader) {
        super(json, reader);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeMemberChangedEvent(generator);
    }

    protected void writeMemberChangedEvent(JsonGenerator generator) throws IOException {
        writeCaseTeamMemberEvent(generator);
    }
}
