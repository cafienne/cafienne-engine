package org.cafienne.cmmn.akka.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * Basic event allowing listeners that are interested only in case team member role events to do initial filtering.
 */
public abstract class CaseTeamRoleEvent extends CaseTeamMemberEvent {
    public final String roleName;

    /**
     * Returns true if the role name is blank
     * @return
     */
    public boolean isMemberItself() {
        return roleName.isBlank();
    }

    protected enum Fields {
        role
    }

    protected CaseTeamRoleEvent(Case caseInstance, MemberKey member, String roleName) {
        super(caseInstance, member);
        this.roleName = roleName;
    }

    protected CaseTeamRoleEvent(ValueMap json) {
        super(json);
        this.roleName = json.raw(Fields.role);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.role, roleName);
    }
}
