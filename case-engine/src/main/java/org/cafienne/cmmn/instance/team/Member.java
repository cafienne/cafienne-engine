package org.cafienne.cmmn.instance.team;

import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.actorapi.event.team.deprecated.TeamMemberAdded;
import org.cafienne.cmmn.actorapi.event.team.member.CaseOwnerAdded;
import org.cafienne.cmmn.actorapi.event.team.member.CaseOwnerRemoved;
import org.cafienne.cmmn.actorapi.event.team.member.TeamRoleCleared;
import org.cafienne.cmmn.actorapi.event.team.member.TeamRoleFilled;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.definition.team.CaseTeamDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A member in the case team. Consists of a user id and associated roles (that have been defined in the {@link CaseTeamDefinition#getCaseRoles()})
 */
public class Member extends CMMNElement<CaseTeamDefinition> {
    private final Team team;
    private final Set<CaseRoleDefinition> roles = new HashSet<>();
    private boolean isOwner = false;
    public final MemberKey key;

    /**
     * Creates a new team member and validates whether the member would fit in the team.
     * Does NOT add the member to the team.
     *
     * @param team
     * @param event
     * @throws CaseTeamError
     */
    Member(Team team, TeamRoleFilled event) throws CaseTeamError {
        this(team, event.key);
    }

    Member(Team team, TeamMemberAdded event) throws CaseTeamError {
        this(team, event.key);
        event.getRoles().forEach(this::addRole);
    }

    protected Member(Team team, MemberKey key) {
        super(team.getCaseInstance(), team.getDefinition());
        this.team = team;
        this.key = key;
    }

    /**
     * Clone construct;
     *
     * @param key
     * @param source
     */
    private Member(MemberKey key, Member source) {
        this(source.team, key);
        this.roles.addAll(source.roles);
        this.isOwner = source.isOwner;
    }

    Member cloneMember(MemberKey newKey) {
        return new Member(newKey, this);
    }

    public boolean isUser() {
        return key.type().equals("user");
    }

    private void addRole(String roleName) {
        CaseRoleDefinition role = getDefinition().getCaseRole(roleName);
        roles.add(role);
    }

    void updateState(TeamRoleFilled event) {
        addRole(event.roleName());
    }

    void updateState(CaseOwnerAdded event) {
        this.isOwner = true;
    }

    void updateState(CaseOwnerRemoved event) {
        this.isOwner = false;
    }

    void updateState(TeamRoleCleared event) {
        CaseRoleDefinition role = findRole(event.roleName());
        if (role != null) {
            roles.remove(role);
        }
    }

    public boolean isOwner() {
        return isOwner;
    }

    public boolean hasRole(String roleName) {
        return findRole(roleName) != null;
    }

    private CaseRoleDefinition findRole(String roleName) {
        return roles.stream().filter(role -> role.getName().equals(roleName)).findFirst().orElse(null);
    }

    /**
     * Returns the user name
     *
     * @return
     */
    public String getMemberId() {
        return key.id();
    }

    /**
     * Adds a role to this team member. Validates first whether this is allowed.
     *
     * @param role
     */
    public void addRole(CaseRoleDefinition role) throws CaseTeamError {
        if (roles.contains(role)) {
            return; // already assigned, no sweat.
        }

        roles.add(role);
    }

    /**
     * Returns the roles currently assigned to this member
     *
     * @return
     */
    public Set<CaseRoleDefinition> getRoles() {
        return roles;
    }

    /**
     * Returns the team that this member belongs to.
     *
     * @return
     */
    public Team getTeam() {
        return team;
    }

    public void dumpMemoryStateToXML(Element parentElement) {
        Element memberXML = parentElement.getOwnerDocument().createElement("Member");
        parentElement.appendChild(memberXML);
        memberXML.setAttribute("name", getMemberId());
        memberXML.setAttribute("roles", roles.toString());
        // roles.forEach(role -> {
        // Element roleXML = parentElement.getOwnerDocument().createElement("Role");
        // memberXML.appendChild(roleXML);
        // roleXML.appendChild(parentElement.getOwnerDocument().createTextNode(role.getName()));
        // });
    }

    @Override
    public void migrateDefinition(CaseTeamDefinition newDefinition) {
        super.migrateDefinition(newDefinition);
        addDebugInfo(() -> "Migrating team member " + key +" (existing roles: " + this.roles +")");
        // Now iterate through the existing roles, and determine which roles are retained and which roles are removed for this member
        //  Note: currently not possible to _add_ roles to a member by means of case definition migration
        Set<CaseRoleDefinition> rolesToRemove = new HashSet<>();
        Set<CaseRoleDefinition> rolesToReplace = new HashSet<>();
        Set<CaseRoleDefinition> rolesToAdd = new HashSet<>();

        Map<String, CaseRoleDefinition> newRoleNames = newDefinition.getCaseRoles().stream().collect(Collectors.toMap(CaseRoleDefinition::getName, role -> role));
        Map<String, CaseRoleDefinition> newRoleIds = newDefinition.getCaseRoles().stream().collect(Collectors.toMap(CaseRoleDefinition::getId, role -> role));

        this.roles.forEach(oldRole -> {
            String name = oldRole.getName();
            String id = oldRole.getId();
            CaseRoleDefinition roleWithMatchingName = newRoleNames.get(name);
            CaseRoleDefinition roleWithMatchingId = newRoleIds.get(id);

            if (roleWithMatchingName != null) {
                addDebugInfo(() -> "- Member " + key +" gets a new version of role " + name +" (with id " + id +")");
                rolesToReplace.add(roleWithMatchingName);
            } else if (roleWithMatchingId != null) {
                addDebugInfo(() -> "- Member " + key +" gets a changed role name: " + name +" becomes " + roleWithMatchingId.getName());
                rolesToRemove.add(oldRole);
                rolesToAdd.add(roleWithMatchingId);
            } else {
                addDebugInfo(() -> "- Member " + key +" drops role: " + name +", as it is no longer part of the case definition");
                rolesToRemove.add(oldRole);
            }
        });
        // Clear the existing array.
        this.roles.clear();
        // Simply dump the roles that have the same name as before (no need to generate events, since events only carry role names)
        this.roles.addAll(rolesToReplace);
        // Generate events for the roles that are no longer in this user
        rolesToRemove.forEach(role -> team.removeMemberRole(key, role.getName()));
        // Generate events for the new roles that the user now gets
        rolesToAdd.forEach(role -> team.addMemberRole(key, role.getName()));
    }
}
