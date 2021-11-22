package org.cafienne.cmmn.actorapi.command.team.member;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to remove a member from the case team, based on the user id of the member.
 *
 */
@Manifest
public class RemoveTeamMember extends CaseTeamMemberCommand {
    public RemoveTeamMember(CaseUserIdentity user, String caseInstanceId, MemberKey key) {
        super(user, caseInstanceId, key);
    }

    public RemoveTeamMember(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        super.validateMembership(caseInstance, key());
        super.validateWhetherOwnerCanBeRemoved(caseInstance, key());
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        Team caseTeam = caseInstance.getCaseTeam();
        caseTeam.removeMember(key());
        return new CaseResponse(this);
    }
}
