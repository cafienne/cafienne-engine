package org.cafienne.cmmn.akka.command.team;

import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Member;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic abstraction for commands relating to CaseTeam.
 */
abstract class CaseTeamCommand extends CaseCommand {
    protected CaseTeamCommand(TenantUser tenantUser, String caseInstanceId) {
        super(tenantUser, caseInstanceId);
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
        CaseRoleDefinition role = caseInstance.getDefinition().getCaseRole(roleName);
        if (role == null) {
            throw new CaseTeamError("A role with name " + roleName + " is not defined within the case");
        }

        Member existingMember = caseInstance.getCaseTeam().getMember(memberId);
        if (existingMember != null) {
            // means, it is an existing member; hence we need to check whether the member does not already have a mutex role
            // Check the mutex roles
            for (CaseRoleDefinition assignedRole : existingMember.getRoles()) {
                if (assignedRole.getMutexRoles().contains(role)) {
                    // not allowed
                    throw new CaseTeamError("Role " + role + " is not allowed for " + memberId + " since " + memberId + " also has role " + assignedRole);
                }
            }
        }

        // Check that a singleton role is not yet assigned to one of the other team members
        if (role.isSingleton()) {
            for (Member member : caseInstance.getCaseTeam().getMembers()) {
                if (!member.key.equals(memberId) && member.hasRole(roleName)) {
                    throw new CaseTeamError("Role " + role + " is already assigned to another user");
                }
            }
        }
    }

    protected void validateCaseTeamRoles(Case caseInstance, CaseTeamMember newMember) {
        newMember.validateRolesExist(caseInstance.getDefinition());

        // Now also validate that the new roles do not mutex each other
        List<CaseRoleDefinition> newRoles = newMember.getCaseRoles().stream().map(caseInstance.getDefinition()::getCaseRole).collect(Collectors.toList());
        newRoles.forEach(role -> {
            newRoles.stream().filter(otherRole -> otherRole != role).forEach(otherRole -> {
                if (otherRole.getMutexRoles().contains(role)) {
                    throw new CaseTeamError("Role " + role + " is not allowed for " + newMember.key() + " since this member also has role " + otherRole);
                }
            });
        });

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
