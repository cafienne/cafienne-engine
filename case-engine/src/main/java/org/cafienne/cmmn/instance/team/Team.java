package org.cafienne.cmmn.instance.team;

import java.util.*;
import java.util.stream.Collectors;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.team.CaseTeam;
import org.cafienne.cmmn.akka.command.team.CaseTeamMember;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.akka.event.team.TeamMemberAdded;
import org.cafienne.cmmn.akka.event.team.TeamMemberRemoved;
import org.w3c.dom.Element;

/**
 * The team of users with their roles that can work on a Case instance.
 * This is an engine extension to CMMN.
 */
public class Team extends CMMNElement<CaseDefinition> {

    private final Collection<Member> members = new ArrayList();

    /**
     * Create a new, empty case team.
     * @param caseInstance
     */
    public Team(Case caseInstance) {
        super(caseInstance, caseInstance.getDefinition());
    }

    /**
     * Create a case team based on the members inside the value map, validated against the case definition.
     * @param newCaseTeam
     * @param caseDefinition
     */
    public Team(CaseTeam newCaseTeam, Case caseInstance, CaseDefinition caseDefinition) throws CaseTeamError {
        this(caseInstance);
        newCaseTeam.getMembers().forEach(caseTeamMember -> members.add(new Member(this, caseTeamMember, caseDefinition)));
    }

    @Deprecated
    public Member addCurrentUser(TenantUser tenantUser) throws CaseTeamError {
        if (tenantUser == null) {
            return null;
        }
        String userId = tenantUser.id();
        for (Member caseTeamMember : members) {
            if (caseTeamMember.getUserId().equals(userId)) {
                return caseTeamMember;
            }
        }

        // TODO MUST be removed => WK: I agree, but apparently demo's will fail when we do.
        List<String> roles = new ArrayList();
        tenantUser.roles().forall(roleName -> {
            // Only add those roles that also have been defined within the case (otherwise new CaseTeamMember constructor will fail)
            if (getCaseInstance().getDefinition().getCaseRole(roleName) != null) {
                roles.add(roleName);
            }
            return true;
        });
        CaseTeamMember newMember = CaseTeamMember.apply(tenantUser.id(), roles.toArray(new String[]{}), true);

        Member member = new Member(this, newMember, getCaseInstance());
        addMember(member);
        return member;
    }

    /**
     * Adds a member to the team
     * @param member
     */
    public void addMember(Member member) {
        getCaseInstance().addEvent(new TeamMemberAdded(getCaseInstance(), member));
    }

    /**
     * Removes a member from the team
     * @param member
     */
    public void removeMember(Member member) {
        getCaseInstance().addEvent(new TeamMemberRemoved(getCaseInstance(), member));
    }

    /**
     * Returns the collection of team members
     * @return
     */
    public Collection<Member> getMembers() {
        return members;
    }

    /**
     * Returns the member with the specified user id or null.
     * @param user
     * @return
     */
    public Member getMember(String user) {
        for (Member member : members) {
            if (member.getUserId().equals(user)) {
                return member;
            }
        }
        return null;
    }

    public void updateState(TeamMemberAdded event) {
        Member newMember = new Member(this, event.getUserId(), event.getRoles(), getCaseInstance());
        members.add(newMember);
    }

    public void updateState(TeamMemberRemoved event) {
        Member member = this.getMember(event.getUserId());
        members.remove(member);
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
    public Member getTeamMember(TenantUser currentTenantUser) {
        String userId = currentTenantUser.id();
        for (Member caseTeamMember : members) {
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
        for (Member caseTeamMember : members) {
            Set<CaseRoleDefinition> roles = caseTeamMember.getRoles();
            for (CaseRoleDefinition roleDefinition : roles) {
                if (roleDefinition.getName().equals(role)) {
                    return caseTeamMember.getUserId();
                }
            }
        }
        return null;
    }

    public CaseTeam toCaseTeamTO() {
        // TODO: this code is invoked for passing caseteam into subcase (from CaseTask). It should also
        //  check whether the roles exist in the subecase, otherwise a failure will happen when starting the subcase
        List<CaseTeamMember> members = new ArrayList();
        this.getMembers().forEach(teamMember -> {
            String[] roleNames = teamMember.getRoles().stream().map(CaseRoleDefinition::getName).collect(Collectors.toList()).toArray(new String[]{});
            CaseTeamMember member = CaseTeamMember.apply(teamMember.getUserId(), roleNames, true);
            members.add(member);
        });
        return CaseTeam.apply(members);
    }

}
