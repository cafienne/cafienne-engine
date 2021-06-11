package org.cafienne.cmmn.actorapi.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Abstraction for commands on individual case team members
 *
 */
abstract class CaseTeamMemberCommand extends CaseTeamCommand {
    protected final String memberId;
    protected final String memberType;

    protected CaseTeamMemberCommand(TenantUser tenantUser, String caseInstanceId, MemberKey key) {
        super(tenantUser, caseInstanceId);
        this.memberId = key.id();
        this.memberType = key.type();
    }

    protected CaseTeamMemberCommand(ValueMap json) {
        super(json);
        this.memberId = readField(json, Fields.memberId);
        this.memberType = readField(json, Fields.memberType);
    }

    public MemberKey key() {
        return new MemberKey(memberId, memberType);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.memberId, memberId);
        writeField(generator, Fields.memberType, memberType);
    }
}
