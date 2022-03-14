package org.cafienne.cmmn.instance.team;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.*;
import org.cafienne.cmmn.actorapi.event.CaseAppliedPlatformUpdate;
import org.cafienne.cmmn.actorapi.event.team.CaseTeamMemberRemoved;
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupAdded;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupChanged;
import org.cafienne.cmmn.actorapi.event.team.group.CaseTeamGroupRemoved;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleAdded;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleChanged;
import org.cafienne.cmmn.actorapi.event.team.tenantrole.CaseTeamTenantRoleRemoved;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserAdded;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserChanged;
import org.cafienne.cmmn.actorapi.event.team.user.CaseTeamUserRemoved;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.definition.team.CaseTeamDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The team of users with their roles that can work on a Case instance.
 * This is an engine extension to CMMN.
 */
public class Team extends CMMNElement<CaseTeamDefinition> {

    private final Map<String, CaseTeamUser> users = new HashMap<>();
    private final Map<String, CaseTeamTenantRole> tenantRoles = new HashMap<>();
    private final Map<String, CaseTeamGroup> groups = new HashMap<>();

    /**
     * Create a new, empty case team.
     *
     * @param caseInstance
     */
    public Team(Case caseInstance) {
        super(caseInstance, caseInstance.getDefinition().getCaseTeamModel());
    }

    public void create(CaseTeam newCaseTeam) {
        replace(newCaseTeam);
    }

    public void replace(CaseTeam newCaseTeam) {
        users.keySet().stream().filter(newCaseTeam::notHasUser).collect(Collectors.toList()).forEach(this::removeUser);
        newCaseTeam.getUsers().forEach(this::setUser);

        groups.keySet().stream().filter(newCaseTeam::notHasGroup).collect(Collectors.toList()).forEach(this::removeGroup);
        newCaseTeam.getGroups().forEach(this::setGroup);

        tenantRoles.keySet().stream().filter(newCaseTeam::notHasTenantRole).collect(Collectors.toList()).forEach(this::removeTenantRole);
        newCaseTeam.getTenantRoles().forEach(this::setTenantRole);
    }

    public CaseTeamUser getUser(String userId) {
        return users.get(userId);
    }

    public CaseTeamTenantRole getTenantRole(String tenantRoleName) {
        return tenantRoles.get(tenantRoleName);
    }

    public CaseTeamGroup getGroup(String groupId) {
        return groups.get(groupId);
    }

    public void upsert(UpsertMemberData memberData) {
        if (memberData.isUser()) {
            CaseTeamUser user = users.get(memberData.id());
            setUser(memberData.asUser(user));
        } else {
            CaseTeamTenantRole role = tenantRoles.get(memberData.id());
            setTenantRole(memberData.asTenantRole(role));
        }
    }

    public void removeUser(String userId) {
        CaseTeamUser user = users.get(userId);
        if (user != null) {
            addEvent(new CaseTeamUserRemoved(this, user));
        }
    }

    public void removeGroup(String groupId) {
        CaseTeamGroup group = groups.get(groupId);
        if (group != null) {
            addEvent(new CaseTeamGroupRemoved(this, group));
        }
    }

    public void removeTenantRole(String tenantRoleName) {
        CaseTeamTenantRole tenantRole = tenantRoles.get(tenantRoleName);
        if (tenantRole != null) {
            addEvent(new CaseTeamTenantRoleRemoved(this, tenantRole));
        }
    }

    private Set<String> getRemovedRoles(CaseTeamMember newMemberInfo, CaseTeamMember existingMember) {
        return existingMember.getCaseRoles().stream().filter(role -> !newMemberInfo.getCaseRoles().contains(role)).collect(Collectors.toSet());
    }

    private boolean hasChangeInfo(CaseTeamMember newMemberInfo, CaseTeamMember existingMember) {
        boolean ownershipChanged = newMemberInfo.isOwner() != existingMember.isOwner();
        Set<String> rolesAdded = newMemberInfo.getCaseRoles().stream().filter(role -> !existingMember.getCaseRoles().contains(role)).collect(Collectors.toSet());
        return (ownershipChanged || ! rolesAdded.isEmpty());
    }

    public void setUser(CaseTeamUser newUserInfo) {
        CaseTeamUser existingUser = users.get(newUserInfo.userId());
        if (existingUser == null) {
            addEvent(new CaseTeamUserAdded(this, newUserInfo));
        } else {
            Set<String> rolesRemoved = getRemovedRoles(newUserInfo, existingUser);
            if (hasChangeInfo(newUserInfo, existingUser) || ! rolesRemoved.isEmpty()) {
                addEvent(new CaseTeamUserChanged(this, newUserInfo, rolesRemoved));
            }
        }
    }

    public void setGroup(CaseTeamGroup newMemberInfo) {
        CaseTeamGroup existingGroup = groups.get(newMemberInfo.groupId());
        if (existingGroup == null) {
            addEvent(new CaseTeamGroupAdded(this, newMemberInfo));
        } else {
            if (existingGroup.differsFrom(newMemberInfo)) {
                Set<GroupRoleMapping> removedMappings = existingGroup.getRemovedMappings(newMemberInfo);
                addEvent(new CaseTeamGroupChanged(this, newMemberInfo, removedMappings));
            }
        }
    }

    public void setTenantRole(CaseTeamTenantRole newMemberInfo) {
        CaseTeamTenantRole existingTenantRole = tenantRoles.get(newMemberInfo.tenantRoleName());
        if (existingTenantRole == null) {
            addEvent(new CaseTeamTenantRoleAdded(this, newMemberInfo));
        } else {
            Set<String> rolesRemoved = getRemovedRoles(newMemberInfo, existingTenantRole);
            if (hasChangeInfo(newMemberInfo, existingTenantRole) || ! rolesRemoved.isEmpty()) {
                addEvent(new CaseTeamTenantRoleChanged(this, newMemberInfo, rolesRemoved));
            }
        }
    }

    /**
     * Returns the collection of team members
     *
     * @return
     */
    public Collection<CaseTeamMember> getMembers() {
        Collection<CaseTeamMember> members = new ArrayList<>();
        members.addAll(users.values());
        members.addAll(tenantRoles.values());
        members.addAll(groups.values());
        return members;
    }

    public Collection<CaseTeamUser> getUsers() {
        return users.values();
    }

    public Collection<CaseTeamGroup> getGroups() {
        return groups.values();
    }

    public Collection<CaseTeamTenantRole> getTenantRoles() {
        return tenantRoles.values();
    }

    /**
     * Returns the collection of owners
     *
     * @return
     */
    public Collection<CaseTeamMember> getOwners() {
        return getMembers().stream().filter(CaseTeamMember::isOwner).collect(Collectors.toList());
    }

    public void updateState(CaseTeamUser user) {
        users.put(user.userId(), user);
    }

    public void updateState(CaseTeamGroup group) {
        groups.put(group.groupId(), group);
    }

    public void updateState(CaseTeamTenantRole tenantRole) {
        tenantRoles.put(tenantRole.tenantRoleName(), tenantRole);
    }

    public void updateState(CaseTeamMemberRemoved<?> event) {
        CaseTeamMember member = event.member;
        String memberId = member.memberId();
        switch (member.memberType()) {
            case TenantRole:
                tenantRoles.remove(memberId);
                break;
            case User:
                users.remove(memberId);
                break;
            case Group:
                groups.remove(memberId);
                break;
        }
    }

    public void updateState(DeprecatedCaseTeamEvent event) {
        if (event.isUserEvent()) {
            CaseTeamUser.handleDeprecatedUserEvent(users, event);
        } else {
            CaseTeamTenantRole.handleDeprecatedTenantRoleEvent(tenantRoles, event);
        }
    }

    /**
     * For debugging purposes mostly.
     *
     * @param parentElement
     */
    public void dumpMemoryStateToXML(Element parentElement) {
        Element caseTeamXML = parentElement.getOwnerDocument().createElement("CaseTeam");
        parentElement.appendChild(caseTeamXML);
//        Element usersElement = caseTeamXML.getOwnerDocument().createElement("users");
//        caseTeamXML.appendChild(usersElement);
//        users.values().forEach(user -> user.dumpMemoryStateToXML(usersElement));
//        Element tenantRolesElement = caseTeamXML.getOwnerDocument().createElement("tenant-roles");
//        caseTeamXML.appendChild(tenantRolesElement);
//        tenantRoles.values().forEach(tenantRole -> tenantRole.dumpMemoryStateToXML(tenantRolesElement));
        // Element groupsElement = caseTeamXML.getOwnerDocument().createElement("groups");
        // caseTeamXML.appendChild(groupsElement);
//        groups.values().forEach(group -> group.dumpMemoryStateToXML(groupsElement));
    }

    /**
     * Returns the first member having the specified role
     *
     * @param role Case Role
     * @return
     */
    public String getMemberWithRole(String role) {
        for (CaseTeamMember caseTeamMember : getMembers()) {
            Set<String> roles = caseTeamMember.getCaseRoles();
            for (String roleName : roles) {
                if (roleName.equals(role)) {
                    return caseTeamMember.memberId();
                }
            }
        }
        return null;
    }

    public CaseTeam createSubCaseTeam(CaseDefinition subCaseDefinition) {
        return CaseTeam.createSubTeam(this, subCaseDefinition.getCaseTeamModel());
    }

    /**
     * Team membership validation method.
     * Is actually not of much use, since all Case and Task actions cannot be send to the case if the
     * tenant user is not part of the case team (see e.g. CaseQueries.authorizeCaseAccess)
     *
     * @param user
     */
    public void validateMembership(CaseUserIdentity user) {
        CurrentMember member = new CurrentMember(this, user);
        if (! member.isValid()) {
            throw new AuthorizationException("User " + user.id() + " is not part of the case team");
        }
    }

    public CurrentMember getTeamMember(CaseUserIdentity currentUser) {
        return new CurrentMember(this, currentUser);
    }

    /**
     * Adds a member to the case team upon request of a Task that has dynamic assignment.
     *
     * @param userId The tenant user id; note that assignment cannot be done on roles, only on users
     * @param role   The (optional) role that the team member must have for executing the task leading to this call
     */
    public void upsertCaseTeamUser(String userId, CaseRoleDefinition role) {
        CaseTeamUser existingMember = users.get(userId);
        if (existingMember == null) {
            addDebugInfo(() -> "Adding unknown user '" + userId + "' to case team because of dynamic task assignment");
            setUser(CaseTeamUser.create(userId, role));
        } else {
            if (!existingMember.isOwner() && role != null && !existingMember.getCaseRoles().contains(role.getName())) {
                addDebugInfo(() -> "Adding case role '" + role.getName() + "' to '" + userId + "' because of task assignment");
                setUser(existingMember.extend(role.getName()));
            }
        }
    }

    public void updateState(CaseAppliedPlatformUpdate event) {
        event.newUserInformation.info().foreach(userInfo -> {
            CaseTeamUser user = getUser(userInfo.existingUserId());
            if (user != null) {
                addDebugInfo(() -> "Replace case team member user id from " + userInfo.existingUserId() + " to " + userInfo.newUserId());
                CaseTeamUser clone = user.cloneUser(userInfo.newUserId());
                users.remove(user.userId());
                users.put(clone.userId(), clone);
            }
            return userInfo;
        });
    }

    @Override
    public void migrateDefinition(CaseTeamDefinition newDefinition) {
        super.migrateDefinition(newDefinition);
        Map<String, CaseRoleDefinition> newRoleNames = newDefinition.getCaseRoles().stream().collect(Collectors.toMap(CaseRoleDefinition::getName, role -> role));
        Map<String, CaseRoleDefinition> newRoleIds = newDefinition.getCaseRoles().stream().collect(Collectors.toMap(CaseRoleDefinition::getId, role -> role));
        Set<String> droppedRoles = new HashSet<>();
        Map<String, String> changedRoleNames = new HashMap<>();

        addDebugInfo(() -> "\nMigrating Case Team");
        getPreviousDefinition().getCaseRoles().forEach(oldRole -> {
            String name = oldRole.getName();
            String id = oldRole.getId();
            CaseRoleDefinition roleWithMatchingName = newRoleNames.get(name);
            CaseRoleDefinition roleWithMatchingId = newRoleIds.get(id);

            if (roleWithMatchingName != null) {
                if (! name.isBlank()) { // There are always "no changes" on blank roles, no need to log it.
                    addDebugInfo(() -> "- role '" + name + "' is not changed");
                }
            } else if (roleWithMatchingId != null) {
                addDebugInfo(() -> "- role '" + name + "' is renamed to '" + roleWithMatchingId.getName() + "'");
                droppedRoles.add(oldRole.getName());
                changedRoleNames.put(oldRole.getName(), roleWithMatchingId.getName());
            } else {
                addDebugInfo(() -> "- role '" + name + "' is dropped");
                droppedRoles.add(oldRole.getName());
            }
        });

        // Inform each member about member-specific changes
        getMembers().forEach(member -> {
            addDebugInfo(() -> "=== Migrating Case Team " + member.memberType() + " '" + member.memberId() + "'");
            member.migrateRoles(this, changedRoleNames, droppedRoles);
        });
        addDebugInfo(() -> "Completed Case Team migration\n");
    }

    @Override
    public String toString() {
        return "Team{" +
                "users=" + users +
                ", tenantRoles=" + tenantRoles +
                ", groups=" + groups +
                '}';
    }
}
