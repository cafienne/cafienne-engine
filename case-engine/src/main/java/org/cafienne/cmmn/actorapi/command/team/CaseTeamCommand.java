package org.cafienne.cmmn.actorapi.command.team;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.json.ValueMap;

/**
 * Generic abstraction for commands relating to CaseTeam.
 */
public abstract class CaseTeamCommand extends CaseCommand {
    protected CaseTeamCommand(CaseUserIdentity user, String caseInstanceId) {
        super(user, caseInstanceId);
    }

    protected CaseTeamCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        if (! caseInstance.getCurrentTeamMember().isOwner()) {
            throw new AuthorizationException("Only case team owners can perform this action");
        }
    }

    protected void validateCaseTeamRole(Case caseInstance, MemberKey memberId, String roleName) {
        CaseRoleDefinition role = caseInstance.getDefinition().getCaseTeamModel().getCaseRole(roleName);
        if (role == null) {
            throw new CaseTeamError("A role with name " + roleName + " is not defined within the case");
        }
    }

    protected void validateCaseTeamRoles(Case caseInstance, CaseTeamMember newMember) {
        newMember.validateRolesExist(caseInstance.getDefinition());

        // First validate the new roles against existing case team
        for (String role : newMember.getCaseRoles()) {
            validateCaseTeamRole(caseInstance, newMember.key(), role);
        }
    }

    protected Member validateMembership(Case caseInstance, MemberKey memberId) {
        Member member = caseInstance.getCaseTeam().getMember(memberId);
        if (member == null) {
            throw new CaseTeamError("The case team does not have a member with id " + memberId);
        }
        return member;
    }

    protected void validateWhetherOwnerCanBeRemoved(Case caseInstance, MemberKey key) {
        Member member = caseInstance.getCaseTeam().getMember(key);
        if (member != null) {
            if (member.isOwner() && caseInstance.getCaseTeam().getOwners().size() == 1) {
                throw new CaseTeamError("Cannot remove case owner " + key);
            }
        }
    }
}
