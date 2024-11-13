/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.actorapi.command.team

import com.casefabric.actormodel.identity.{CaseUserIdentity, ConsentGroupMembership, Origin}
import com.casefabric.cmmn.definition.team.CaseRoleDefinition
import com.casefabric.cmmn.instance.team.Team

import scala.jdk.CollectionConverters.CollectionHasAsScala

class CurrentMember(team: Team, user: CaseUserIdentity) extends CaseTeamUser {
  lazy val isValid: Boolean = userMembership.nonEmpty || tenantRoleMembership.nonEmpty || groupMembership.nonEmpty
  private lazy val getRoles: Set[CaseRoleDefinition] = (userCaseRoles ++ roleMembers ++ userGroupCaseRoles).map(team.getDefinition.getCaseRole)
  private lazy val userCaseRoles = userMembership.flatMap(_.caseRoles)
  private lazy val roleMembers = tenantRoleMembership.flatMap(_.caseRoles)
  private lazy val userGroupCaseRoles: Set[String] = groupMembership.flatMap(_.caseRoles)
  private lazy val userMembership: Set[CaseTeamUser] = team.getUsers.asScala.filter(_.userId == userId).toSet
  private lazy val tenantRoleMembership: Set[CaseTeamTenantRole] = team.getTenantRoles.asScala.filter(member => user.tenantRoles.contains(member.tenantRoleName)).toSet
  private lazy val groupMembership: Set[GroupRoleMapping] = {
    val userGroups: Seq[ConsentGroupMembership] = user.groups.filter(group => team.getGroups.asScala.exists(_.groupId == group.groupId))
    val teamGroups: Iterable[CaseTeamGroup] = team.getGroups.asScala.filter(group => user.groups.map(_.groupId).contains(group.groupId))
    teamGroups.flatMap(group => {
      val isGroupOwner = groupsOwnedByThisUser.map(_.groupId).contains(group.groupId)
      if (isGroupOwner) {
        group.mappings // Group owners have all group roles and corresponding case roles
      } else {
        // Others just have the case roles that belong to their group roles.
        val userGroupRoles = userGroups.filter(_.groupId == group.groupId).flatMap(_.roles)
        group.mappings.filter(mapping => mapping.groupRole.isEmpty || userGroupRoles.contains(mapping.groupRole))
      }
    }).toSet
  }
  // Note!!! at the end of next line we have filter on not null.
  //  There is a particular reason for this: the current member retrieves its groups from the query db, and that db may be
  //  out of sync with the current status of the case team (e.g. because of parallel removal of a group)
  //  The query db then thinks the group still belongs to the case team, causing null-pointers when "groupsOwnedByThisUser".[something] is accessed
  private lazy val groupsOwnedByThisUser: Seq[CaseTeamGroup] = user.groups.filter(_.isOwner).map(_.groupId).map(team.getGroup).filter(_ != null)
  override val userId: String = user.id
  override val origin: Origin = user.origin
  override val caseRoles: Set[String] = getRoles.map(_.getName)
  override val isOwner: Boolean = userMembership.exists(_.isOwner) || tenantRoleMembership.exists(_.isOwner) || groupMembership.exists(_.isOwner)

  def hasRoles(caseRoles: java.util.Collection[CaseRoleDefinition]): Boolean = {
    if (caseRoles.isEmpty) {
      true // No roles defined, any team member can perform the related action (plan discretionary item or raise user event).
    } else {
      // Check whether I (as current user) have one of the roles in the list, potentially because i am case owner or group owner.
      caseRoles.asScala.exists(hasRole)
    }
  }

  def hasRole(caseRole: CaseRoleDefinition): Boolean = {
    if (caseRole == null) {
      true // Well, if no role is defined, we can perform the action (typically complete a task)
    } else if (getRoles.contains(caseRole)) {
      true // If one of our roles matches the case role it is fine too.
    } else if (isRoleManager(caseRole)) {
      true // If we manage the role (through case ownership or group ownership on a group-exclusive role)
    } else {
      false
    }
  }

  def isRoleManager(caseRole: CaseRoleDefinition): Boolean = {
    if (caseRole == null) {
      // - If there is no role defined, and we are a case owner, then also can we manage this operation (e.g. assign a task)
      isOwner
    } else if (groupsOwnedByThisUser.exists(_.caseRoles.contains(caseRole.getName))) {
      // - If we're a group owner and the role is in the group
      true
    } else if (isOwner) {
      // - If you're a case owner
      // - and the role is not set to a group,
      // - or, the role is also available on tenant membership
      val groupsThatFillThisCaseRole = team.getGroups.asScala.filter(_.mappings.exists(_.caseRoles.contains(caseRole.getName)))
      if (groupsThatFillThisCaseRole.isEmpty) {
        // If there are no groups associated with this role, we also manage it because we have case ownership
        true
      } else {
        // Ok, so there are groups associated with the case role.
        //  But, since we're case owner, if also tenant role or tenant user based membership has this role, then we also manage the role
        // NOTE: we're checking only tenant users!!!! Other users in the team can be there because of IDP or ConsentGroup based membership, but that means it is not tenant based membership
        val has_a_CaseTeamUserWithThisCaseRole: Boolean = team.getUsers.asScala.filter(_.origin == Origin.Tenant).exists(_.caseRoles.contains(caseRole.getName))
        val has_a_TenantRoleWithThisCaseRole: Boolean = team.getTenantRoles.asScala.exists(_.caseRoles.contains(caseRole.getName))
        has_a_CaseTeamUserWithThisCaseRole || has_a_TenantRoleWithThisCaseRole
      }
    } else {
      // Nope, we're not managing the role
      false
    }
  }
}
