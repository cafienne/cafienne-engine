package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.actormodel.identity.{CaseUserIdentity, ConsentGroupMembership, Origin}
import org.cafienne.cmmn.definition.team.CaseRoleDefinition
import org.cafienne.cmmn.instance.team.Team

import scala.jdk.CollectionConverters.CollectionHasAsScala

class CurrentMember(team: Team, user: CaseUserIdentity) extends CaseTeamUser {
  lazy val isValid: Boolean = {
    userMembership.nonEmpty || tenantRoleMembership.nonEmpty || groupMembership.nonEmpty
  }
  private lazy val getRoles: Set[CaseRoleDefinition] = {
    val userCaseRoles = userMembership.flatMap(_.caseRoles)
    val roleMembers = tenantRoleMembership.flatMap(_.caseRoles)
    val userGroupCaseRoles: Set[String] = groupMembership.flatMap(_.caseRoles)
    (userCaseRoles ++ roleMembers ++ userGroupCaseRoles).map(team.getDefinition.getCaseRole)
  }
  private lazy val userMembership: Set[CaseTeamUser] = {
    team.getUsers.asScala.filter(_.userId == userId).toSet
  }
  private lazy val groupMembership: Set[GroupRoleMapping] = {
    val userGroups: Seq[ConsentGroupMembership] = user.groups.filter(group => team.getGroups.asScala.exists(_.groupId == group.groupId))
    val teamGroups: Iterable[CaseTeamGroup] = team.getGroups.asScala.filter(group => user.groups.map(_.groupId).contains(group.groupId))
    teamGroups.flatMap(group => {
      val userGroupRoles = userGroups.filter(_.groupId == group.groupId).flatMap(_.roles)
      group.mappings.filter(mapping => userGroupRoles.contains(mapping.groupRole))
    }).toSet
  }
  private lazy val tenantRoleMembership: Set[CaseTeamTenantRole] = {
    team.getTenantRoles.asScala.filter(member => user.tenantRoles.contains(member.tenantRoleName)).toSet
  }
  override val userId: String = user.id
  override val origin: Origin = user.origin
  override val isOwner: Boolean = {
    userMembership.exists(_.isOwner) || tenantRoleMembership.exists(_.isOwner) || groupMembership.exists(_.isOwner)
  }

  def hasRoles(caseRoles: java.util.Collection[CaseRoleDefinition]): Boolean = {
    if (caseRoles.isEmpty) {
      // Ouch. Anyone can take this task or discretionary item - no roles defined.
      true
    } else if (isOwner) {
      // Case owners can do all things they want. But is that good?
      true
    } else {
      caseRoles.asScala.exists(hasRole)
    }
  }

  def hasRole(caseRole: CaseRoleDefinition): Boolean = {
    if (caseRole == null) {
      true
    } else if (isOwner) {
      true
    } else {
      getRoles.contains(caseRole)
    }
  }
}
