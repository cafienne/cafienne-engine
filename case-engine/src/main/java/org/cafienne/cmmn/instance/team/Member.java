package org.cafienne.cmmn.instance.team;

import org.cafienne.cmmn.akka.command.team.CaseTeamMember;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * A member in the case team. Consists of a user id and associated roles (that have been defined in the {@link CaseDefinition#getCaseRoles()})
 */
public class Member extends CMMNElement<CaseDefinition> {
    private final Team team;
    private final String userId;
    private final Set<CaseRoleDefinition> roles = new HashSet();

    /**
     * Shortcut constructor for akka recovery
     *
     * @param team
     * @param userId
     * @param roles
     * @param caseInstance 
     */
    Member(Team team, String userId, Set<String> roles, Case caseInstance) {
        super(team, team.getDefinition());
        this.team = team;
        this.userId = userId;
        roles.add("");// Always add empty role for members
        for (String roleName : roles) {
            CaseRoleDefinition role = caseInstance.getDefinition().getCaseRole(roleName);
            this.roles.add(role);
        }
    }

    /**
     * Creates a new team member and validates whether the member would fit in the team.
     * Does NOT add the member to the team.
     *
     * @param team
     * @param member
     * @param caseInstance
     * @throws CaseTeamError
     */
    public Member(Team team, CaseTeamMember member, Case caseInstance) throws CaseTeamError {
        this(team, member, caseInstance.getDefinition());
    }

    /**
     * Creates a new team member and validates whether the member would fit in the team.
     * Does NOT add the member to the team.
     *
     * @param team
     * @param member
     * @param caseDefinition
     * @throws CaseTeamError
     */
    public Member(Team team, CaseTeamMember member, CaseDefinition caseDefinition) throws CaseTeamError {
        super(team, caseDefinition);
        this.team = team;
        this.userId = parseName(member.getUser());
        member.getRoles().forEach(roleName -> addRole(getCaseRole(caseDefinition, roleName)));
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
    public String getUserId() {
        return userId;
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
                throw new CaseTeamError("Role " + role + " is not allowed for " + getUserId() + " since " + getUserId() + " also has role " + assignedRole);
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
        memberXML.setAttribute("name", getUserId());
        memberXML.setAttribute("roles", roles.toString());
        // roles.forEach(role -> {
        // Element roleXML = parentElement.getOwnerDocument().createElement("Role");
        // memberXML.appendChild(roleXML);
        // roleXML.appendChild(parentElement.getOwnerDocument().createTextNode(role.getName()));
        // });
    }
}
