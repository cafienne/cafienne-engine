package org.cafienne.cmmn.akka.command.team;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.akka.actor.identity.TenantUser;

/**
 * Command to remove a member from the case team, based on the user id of the member.
 *
 */
@Manifest
public class RemoveTeamMember extends CaseCommand {

    private final String userId;
    private Member memberToRemove;

    private enum Fields {
        userId
    }

    public RemoveTeamMember(TenantUser tenantUser, String caseInstanceId, String userId) {
        // TODO: determine how to do authorization on this command.
        super(tenantUser, caseInstanceId);
        this.userId = userId;
    }

    public RemoveTeamMember(ValueMap json) {
        super(json);
        this.userId = readField(json, Fields.userId);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);

        Collection<Member> currentMembers = caseInstance.getCaseTeam().getMembers();
        for (Member caseTeamMember : currentMembers) {
            if (caseTeamMember.getUserId().equals(userId)) {
                memberToRemove = caseTeamMember; // Gotcha!
                return;
            }
        }
        throw new InvalidCommandException("User " + userId + " cannot be removed from the case team");
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        Team caseTeam = caseInstance.getCaseTeam();
        caseTeam.removeMember(memberToRemove);
        return new CaseResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.userId, userId);
    }
}
