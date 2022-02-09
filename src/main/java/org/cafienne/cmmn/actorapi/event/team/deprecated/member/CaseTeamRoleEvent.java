package org.cafienne.cmmn.actorapi.event.team.deprecated.member;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member role events to do initial filtering.
 */
public abstract class CaseTeamRoleEvent extends DeprecatedCaseTeamEvent {
    private final String roleName;

    /**
     * Returns true if the role name is blank
     * @return
     */
    public boolean isMemberItself() {
        return roleName.isBlank();
    }

    protected CaseTeamRoleEvent(ValueMap json) {
        super(json);
        this.roleName = json.readString(Fields.role);
    }

    public String roleName() {
        return roleName;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, roleName);
    }
}
