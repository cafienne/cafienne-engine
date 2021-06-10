package org.cafienne.cmmn.actorapi.command.team;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.team.Team;

/**
 * Command to remove a member from the case team, based on the user id of the member.
 *
 */
@Manifest
public class RemoveTeamMember extends CaseTeamMemberCommand {
    public RemoveTeamMember(TenantUser tenantUser, String caseInstanceId, MemberKey key) {
        super(tenantUser, caseInstanceId, key);
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
