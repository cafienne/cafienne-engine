package org.cafienne.cmmn.user;

import java.util.*;
import java.util.stream.Collectors;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.akka.event.team.TeamMemberAdded;
import org.cafienne.cmmn.akka.event.team.TeamMemberRemoved;
import org.w3c.dom.Element;

/**
 * The team of users with their roles that can work on a Case instance.
 * This is an engine extension to CMMN.
 */
public class CaseTeam {

    private final Collection<CaseTeamMember> members = new ArrayList<>();
    private final Case caseInstance;

    /**
     * Create a new, empty case team.
     * @param caseInstance
     */
    public CaseTeam(Case caseInstance) {
        this.caseInstance = caseInstance;
    }

    /**
     * Create a case team based on the members inside the value map, validated against the case definition.
     * @param caseTeam
     * @param caseDefinition
     */
    public CaseTeam(org.cafienne.cmmn.akka.command.team.CaseTeam caseTeam, Case caseInstance, CaseDefinition caseDefinition) throws CaseTeamError {
        this(caseInstance);
        caseTeam.getMembers().forEach(caseTeamMember -> members.add(new CaseTeamMember(this, caseTeamMember, caseDefinition)));
    }

    @Deprecated
    public CaseTeamMember addCurrentUser(TenantUser tenantUser) throws CaseTeamError {
        if (tenantUser == null) {
            return null;
        }
        String userId = tenantUser.id();
        for (CaseTeamMember caseTeamMember : members) {
            if (caseTeamMember.getUserId().equals(userId)) {
                return caseTeamMember;
            }
        }

        // TODO MUST be removed => WK: I agree, but apparently demo's will fail when we do.
        org.cafienne.cmmn.akka.command.team.CaseTeamMember newMember = new org.cafienne.cmmn.akka.command.team.CaseTeamMember(tenantUser.id());
        tenantUser.roles().forall(roleName -> {
            // Only add those roles that also have been defined within the case (otherwise new CaseTeamMember constructor will fail)
            if (this.caseInstance.getDefinition().getCaseRole(roleName) != null) {
                newMember.getRoles().add(roleName);
            }
            return true;
        });

        CaseTeamMember member = new CaseTeamMember(this, newMember, this.caseInstance);
        addMember(member);
        return member;
    }

    /**
     * Adds a member to the team
     * @param member
     */
    public void addMember(CaseTeamMember member) {
        members.add(member);
        caseInstance.storeInternallyGeneratedEvent(new TeamMemberAdded(caseInstance, member)).finished();
    }

    /**
     * Removes a member from the team
     * @param member
     */
    public void removeMember(CaseTeamMember member) {
        members.remove(member);
        caseInstance.storeInternallyGeneratedEvent(new TeamMemberRemoved(caseInstance, member)).finished();
    }

    /**
     * Returns the collection of team members
     * @return
     */
    public Collection<CaseTeamMember> getMembers() {
        return members;
    }

    /**
     * Returns the member with the specified user id or null.
     * @param user
     * @return
     */
    public CaseTeamMember getMember(String user) {
        for (CaseTeamMember member : members) {
            if (member.getUserId().equals(user)) {
                return member;
            }
        }
        return null;
    }

    /**
     * Akka recovery method - does no validations.
     * @param user
     * @param roles
     */
    public void recoverMember(String user, Set<String> roles) {
        addMember(new CaseTeamMember(this, user, roles, caseInstance));
    }

    /**
     * For debugging purposes mostly.
     * @param parentElement
     */
    public void dumpMemoryStateToXML(Element parentElement) {
        Element caseTeamXML = parentElement.getOwnerDocument().createElement("CaseTeam");
        parentElement.appendChild(caseTeamXML);
        members.forEach(member -> {
            member.dumpMemoryStateToXML(caseTeamXML);
        });
    }

    @Deprecated
    public CaseTeamMember getTeamMember(TenantUser currentTenantUser) {
        String userId = currentTenantUser.id();
        for (CaseTeamMember caseTeamMember : members) {
            if (caseTeamMember.getUserId().equals(userId)) {
                return caseTeamMember;
            }
        }
        // TODO: implement this
        return null;
    }

    /**
     * Returns the first member having the specified role
     * @param role Case Role
     * @return
     */
    public String getMemberWithRole(String role) {
        for (CaseTeamMember caseTeamMember : members) {
            Set<CaseRoleDefinition> roles = caseTeamMember.getRoles();
            for (CaseRoleDefinition roleDefinition : roles) {
                if (roleDefinition.getName().equals(role)) {
                    return caseTeamMember.getUserId();
                }
            }
        }
        return null;
    }

    public org.cafienne.cmmn.akka.command.team.CaseTeam toCaseTeamTO() {
        List<org.cafienne.cmmn.akka.command.team.CaseTeamMember> members = new ArrayList<>();
        this.getMembers().forEach(teamMember -> members.add(new org.cafienne.cmmn.akka.command.team.CaseTeamMember(teamMember.getUserId(), teamMember.getRoles().stream().map(CaseRoleDefinition::getName).collect(Collectors.toSet()))));
        return new org.cafienne.cmmn.akka.command.team.CaseTeam(members);
    }
}
