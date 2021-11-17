package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}
import org.cafienne.cmmn.definition.team.CaseRoleDefinition
import org.cafienne.cmmn.instance.team.Team

import scala.jdk.CollectionConverters.{CollectionHasAsScala, SetHasAsJava}

class CurrentMember(team: Team, user: CaseUserIdentity) extends CaseTeamUser {
  lazy val isValid: Boolean = {
    userMembership.nonEmpty || tenantRoleMembership.nonEmpty
  }
  lazy val getRoles: java.util.Set[CaseRoleDefinition] = {
    val userCaseRoles = userMembership.flatMap(_.caseRoles)
    val roleMembers = tenantRoleMembership.flatMap(_.caseRoles)
    (userCaseRoles ++ roleMembers).map(team.getDefinition.getCaseRole).asJava
  }
  private lazy val userMembership: Set[CaseTeamUser] = {
    team.getUsers.asScala.filter(_.userId == userId).toSet
  }
  private lazy val tenantRoleMembership: Set[CaseTeamTenantRole] = {
    team.getTenantRoles.asScala.filter(member => user.tenantRoles.contains(member.tenantRoleName)).toSet
  }
  override val userId: String = user.id
  override val origin: Origin = user.origin
  override val isOwner: Boolean = {
    userMembership.exists(_.isOwner) || tenantRoleMembership.exists(_.isOwner)
  }
}
