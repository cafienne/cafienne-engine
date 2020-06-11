package org.cafienne.cmmn.akka.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Member;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class CaseTeamMemberEvent extends CaseTeamEvent {
    public final String memberId;
    public final boolean isTenantUser;

    protected enum Fields {
        memberId, isTenantUser
    }

    protected CaseTeamMemberEvent(Case caseInstance, Member member) {
        super(caseInstance);
        this.memberId = member.getUserId();
        this.isTenantUser = true;
    }

    protected CaseTeamMemberEvent(ValueMap json) {
        super(json);
        this.memberId = json.raw(Fields.memberId);
        this.isTenantUser = json.raw(Fields.isTenantUser);
    }

    protected String getMemberDescription() {
        String type = isTenantUser ? "Tenant user " : "Tenant role ";
        return type + "'" + memberId + "'";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.memberId, memberId);
        writeField(generator, Fields.isTenantUser, isTenantUser);
    }
}
