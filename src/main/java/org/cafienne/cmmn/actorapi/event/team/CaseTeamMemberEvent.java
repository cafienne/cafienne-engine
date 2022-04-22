package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class CaseTeamMemberEvent<Member extends CaseTeamMember> extends CaseTeamEvent {
    public final Member member;

    protected CaseTeamMemberEvent(Team team, Member newInfo) {
        super(team);
        this.member = newInfo;
    }

    protected CaseTeamMemberEvent(ValueMap json, CaseTeamMemberDeserializer<Member> reader) {
        super(json);
        this.member = reader.readMember(json.with(Fields.member));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        writeCaseTeamMemberEvent(generator);
    }

    protected void writeCaseTeamMemberEvent(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.member, member);
    }
}

