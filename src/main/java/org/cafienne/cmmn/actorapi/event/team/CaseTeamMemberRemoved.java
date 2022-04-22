package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMemberDeserializer;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class CaseTeamMemberRemoved<Member extends CaseTeamMember> extends CaseTeamMemberEvent<Member> {

    protected CaseTeamMemberRemoved(Team team, Member member) {
        super(team, member);
    }

    protected CaseTeamMemberRemoved(ValueMap json, CaseTeamMemberDeserializer<Member> reader) {
        super(json, reader);
    }

    @Override
    protected void updateState(Team team) {
        team.updateState(this);
    }

    protected void writeCaseTeamMemberEvent(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.member, member.memberKeyJson());
    }
}
