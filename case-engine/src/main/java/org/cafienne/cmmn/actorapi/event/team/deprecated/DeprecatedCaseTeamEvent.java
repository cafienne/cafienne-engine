package org.cafienne.cmmn.actorapi.event.team.deprecated;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * TeamMemberAdded and TeamMemberRemoved are no longer generated
 */
public abstract class DeprecatedCaseTeamEvent extends CaseTeamEvent {
    public final String memberId;
    public final boolean isTenantUser;

    protected DeprecatedCaseTeamEvent(ValueMap json) {
        super(json);
        this.memberId = json.readString(Fields.memberId);
        this.isTenantUser = json.readBoolean(Fields.isTenantUser, true);
    }

    public boolean isUserEvent() {
        return isTenantUser;
    }

    public String getId() {
        return memberId;
    }

    public String roleName() {
        return "";
    }

    protected String getMemberDescription() {
        return "Tenant " + (isTenantUser ? "user " : "role ") + getId();
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.memberId, memberId);
        writeField(generator, Fields.isTenantUser, isTenantUser);
    }

}
