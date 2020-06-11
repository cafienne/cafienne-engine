package org.cafienne.cmmn.instance.team;

import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.akka.event.team.*;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * A member in the case team. Consists of a user id and associated roles (that have been defined in the {@link CaseDefinition#getCaseRoles()})
 */
public class Member extends CMMNElement<CaseDefinition> {
    private final Team team;
    private final Set<CaseRoleDefinition> roles = new HashSet();
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
        event.getRoles().forEach(roleName -> addRole(roleName));
    }

    protected Member(Team team, MemberKey key) {
        super(team, team.getDefinition());
        this.team = team;
        this.key = key;
    }

    boolean isUser() {
        return key.type().equals("user");
    }

    private void addRole(String roleName) {
        CaseRoleDefinition role = getCaseInstance().getDefinition().getCaseRole(roleName);
        roles.add(role);
    }

    void updateState(TeamRoleFilled event) {
        addRole(event.roleName);
    }

    void updateState(CaseOwnerAdded event) {
        this.isOwner = true;
    }

    void updateState(CaseOwnerRemoved event) {
        this.isOwner = false;
    }

    void updateState(TeamRoleCleared event) {
        CaseRoleDefinition role = findRole(event.roleName);
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
     * Get the role after validating against case definition
     * @param caseDefinition
     * @param roleName
     * @return
     * @throws CaseTeamError
     */
    private CaseRoleDefinition getCaseRole(CaseDefinition caseDefinition, String roleName) throws CaseTeamError {
        CaseRoleDefinition role = caseDefinition.getCaseRole(roleName);
        if (role == null) {
            throw new CaseTeamError("A role with name " + roleName + " is not defined within the case");
            // Alternatively we could just ignore the role?
        }
        return role;
    }

    /**
     * Parse and validate the user name
     *
     * @param nameValue
     * @return
     */
    private String parseName(String nameValue) throws CaseTeamError {
        if (nameValue == null) {
            throw new CaseTeamError("The user is not supplied");
        }
        return nameValue;
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

        // Check the mutex roles
        for (CaseRoleDefinition assignedRole : roles) {
            if (assignedRole.getMutexRoles().contains(role)) {
                // not allowed
                throw new CaseTeamError("Role " + role + " is not allowed for " + getMemberId() + " since " + getMemberId() + " also has role " + assignedRole);
            }
        }

        // Check that a singleton role is not yet assigned to one of the other team members
        if (role.isSingleton()) {
            for (Member member : getTeam().getMembers()) {
                if (member.getRoles().contains(role)) {
                    throw new CaseTeamError("Role " + role + " is already assigned to another user");
                }
            }
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
    public CaseDefinition getDefinition() {
        return getCaseInstance().getDefinition();
    }
}
