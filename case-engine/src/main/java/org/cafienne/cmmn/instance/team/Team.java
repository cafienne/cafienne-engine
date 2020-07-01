package org.cafienne.cmmn.instance.team;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.team.CaseTeam;
import org.cafienne.cmmn.akka.command.team.CaseTeamMember;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.akka.event.team.*;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    public void clear() {
        addDebugInfo(() -> "Clearing existing case team");
        getMembers().stream().map(member -> member.key).collect(Collectors.toList()).forEach(this::removeMember);
    }

    public void fillFrom(CaseTeam newCaseTeam) {
        newCaseTeam.getMembers().forEach(this::upsert);
    }

    public void upsert(CaseTeamMember newMemberInfo) {
        MemberKey key = newMemberInfo.key();
        Member member = getMember(key);
        if (member == null) {
            // We need to add a new member;
            // Always add an empty role
            getCaseInstance().addEvent(new TeamRoleFilled(getCaseInstance(), key, ""));
        }
        newMemberInfo.getCaseRoles().forEach(caseRole -> {
            if (!getMember(key).hasRole(caseRole)) {
                getCaseInstance().addEvent(new TeamRoleFilled(getCaseInstance(), key, caseRole));
            } else {
                addDebugInfo(() -> "Ignoring request to add case role '" + caseRole + "' to member '" + key + "' since the member already has this role");
            }
        });

        if (newMemberInfo.isOwner().nonEmpty()) {
            addDebugInfo(() -> "Updating ownership information for member " + key);
            if (newMemberInfo.isOwner().getOrElse(() -> false)) {
                addOwner(key);
            } else {
                removeOwner(key);
            }
        }

        if (newMemberInfo.removeRoles().nonEmpty()) {
            addDebugInfo(() -> "Removing roles from member " + key);
            newMemberInfo.rolesToRemove().forEach(roleName -> getCaseInstance().addEvent(new TeamRoleCleared(getCaseInstance(), key, roleName)));
        }
    }

    /**
     * Removes a member from the team
     *
     * @param key
     */
    public void removeMember(MemberKey key) {
        Member member = getMember(key);
        if (member != null) {
            // Make sure to check and remove ownership as well
            if (member.isOwner()) {
                removeOwner(key);
            }
            new ArrayList<>(member.getRoles()).forEach(role -> getCaseInstance().addEvent(new TeamRoleCleared(getCaseInstance(), member.key, role.getName())));
            getCaseInstance().addEvent(new TeamRoleCleared(getCaseInstance(), member.key, ""));
        } else {
            addDebugInfo(() -> "Cannot remove case team member '" + key.id() + "', since this member is not in the team");
        }
    }

    /**
     * Adds a member to the team
     *
     * @param key
     */
    public void addOwner(MemberKey key) {
        addDebugInfo(() -> "Trying to add ownership for member " + key);
        Member member = getMember(key);
        if (member.isOwner()) {
            // No need to add owner again
            addDebugInfo(() -> "Member is already owner");
            return;
        }
        getCaseInstance().addEvent(new CaseOwnerAdded(getCaseInstance(), key));
    }

    /**
     * Adds a member to the team
     *
     * @param key
     */
    public void removeOwner(MemberKey key) {
        addDebugInfo(() -> "Trying to remove ownership for member " + key);
        Member member = getMember(key);
        if (member.isOwner()) {
            getCaseInstance().addEvent(new CaseOwnerRemoved(getCaseInstance(), key));
        } else {
            addDebugInfo(() -> "Member is not an owner");
        }
    }

    /**
     * Returns the collection of team members
     * @return
     */
    public Collection<Member> getMembers() {
        return members;
    }

    /**
     * Returns the collection of owners
     *
     * @return
     */
    public Collection<Member> getOwners() {
        return members.stream().filter(member -> member.isOwner()).collect(Collectors.toList());
    }

    /**
     * Returns the member with the specified key or null.
     * @param key
     * @return
     */
    public Member getMember(MemberKey key) {
        for (Member member : members) {
            if (member.key.equals(key)) {
                return member;
            }
        }
        return null;
    }

    public void updateState(CaseOwnerAdded event) {
        getMember(event.key).updateState(event);
    }

    public void updateState(CaseOwnerRemoved event) {
        getMember(event.key).updateState(event);
    }

    public void updateState(TeamRoleCleared event) {
        if (event.isMemberItself()) {
            members.remove(getMember(event.key));
        } else {
            getMember(event.key).updateState(event);
        }
    }

    public void updateState(TeamRoleFilled event) {
        if (event.isMemberItself()) {
            members.add(new Member(this, event));
        } else {
            getMember(event.key).updateState(event);
        }
    }

    public void updateState(TeamMemberAdded event) {
        Member newMember = new Member(this, event);
        members.add(newMember);
    }

    public void updateState(TeamMemberRemoved event) {
        Member member = this.getMember(event.key);
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
                    return caseTeamMember.getMemberId();
                }
            }
        }
        return null;
    }

    public CaseTeam createSubCaseTeam(CaseDefinition subCaseDefinition) {
        List<CaseTeamMember> members = new ArrayList();
        this.getMembers().forEach(teamMember -> {
            String[] roleNames = teamMember.getRoles().stream().map(CaseRoleDefinition::getName).filter(name -> subCaseDefinition.getCaseRole(name)!=null).collect(Collectors.toList()).toArray(new String[]{});
            CaseTeamMember member = CaseTeamMember.apply(teamMember.key, roleNames, teamMember.isOwner());
            members.add(member);
        });
        return CaseTeam.apply(members);
    }

    /**
     * Team membership validation method.
     * Is actually not of much use, since all Case and Task actions cannot be send to the case if the
     * tenant user is not part of the case team (see e.g. CaseQueries.authorizeCaseAccess)
     *
     * @param user
     */
    public void validateMembership(TenantUser user) {
        for (Member member : members) {
            if (member.isUser()) {
                if (user.id().equals(member.key.id())) {
                    return;
                }
            } else {
                // Member is a tenant role, check whether the user has it
                if (user.roles().contains(member.key.id())) {
                    return;
                }
            }
        }

        throw new SecurityException("User " + user.id() + " is not part of the case team");
    }

    public CurrentMember getTeamMember(TenantUser currentTenantUser) {
        return new CurrentMember(this, currentTenantUser);
    }

    @Override
    public CaseDefinition getDefinition() {
        return getCaseInstance().getDefinition();
    }

    /**
     * Adds a member to the case team upon request of a Task that has dynamic assignment.
     * @param assignee
     * @param performer
     */
    public void addDynamicMember(String assignee, CaseRoleDefinition performer) {
        List<Member> existingMembers = getMembers().stream().filter(member -> member.isUser() && member.key.id().equals(assignee)).collect(Collectors.toList());
        if (existingMembers.isEmpty()) {
            // Add the member; as a member, and if a role is required also with the role
            getCaseInstance().addDebugInfo(() -> "Adding team member '" + assignee +"' because of dynamic task assignment");
            getCaseInstance().addEvent(new TeamRoleFilled(getCaseInstance(), new MemberKey(assignee, "user"), ""));
            if (performer != null) getCaseInstance().addEvent(new TeamRoleFilled(getCaseInstance(), new MemberKey(assignee, "user"), performer.getName()));
        } else {
            // If a role is required, then check if "one of" the existing members (there can only be one, actually) has the role; if not, give the role.
            if (performer != null) {
                Member member = existingMembers.get(0);
                if (! member.hasRole(performer.getName())) {
                    getCaseInstance().addDebugInfo(() -> "Adding case role '" + performer.getName()+"' to '" + assignee +"' because of dynamic task assignment");
                    getCaseInstance().addEvent(new TeamRoleFilled(getCaseInstance(), new MemberKey(assignee, "user"), performer.getName()));
                }
            }
        }
    }
}
