package org.cafienne.cmmn.akka.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.akka.actor.serialization.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member events to do initial filtering.
 */
public abstract class CaseTeamMemberEvent extends CaseTeamEvent {
    public final String memberId;
    public final boolean isTenantUser;
    public final transient MemberKey key;

    protected CaseTeamMemberEvent(Case caseInstance, MemberKey key) {
        super(caseInstance);
        this.key = key;
        this.memberId = key.id();
        this.isTenantUser = key.type().equals("user");
    }

    protected CaseTeamMemberEvent(ValueMap json) {
        super(json);
        this.memberId = json.raw(Fields.memberId);
        this.isTenantUser = json.raw(Fields.isTenantUser);
        this.key = new MemberKey(memberId, isTenantUser ? "user" : "role");
    }

    public String roleName() {
        return "";
    }

    protected String getMemberDescription() {
        return "Tenant " + key;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.memberId, memberId);
        writeField(generator, Fields.isTenantUser, isTenantUser);
    }
}
