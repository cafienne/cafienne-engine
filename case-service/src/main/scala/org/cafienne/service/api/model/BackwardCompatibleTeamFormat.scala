package org.cafienne.service.api.model

import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamTenantRole, CaseTeamUser}

case class BackwardCompatibleTeamFormat(
           users: Seq[CaseTeamFormat.CaseTeamUserFormat] = Seq(),
           tenantRoles: Seq[CaseTeamFormat.TenantRoleFormat] = Seq(),
           members: Seq[BackwardCompatibleTeamMemberFormat] = Seq()) {
  private lazy val getUsers: Seq[CaseTeamUser] = users.map(_.asCaseTeamUser) ++ members.filter(_.isUser).map(_.asUser)
  private lazy val getRoles: Seq[CaseTeamTenantRole] = tenantRoles.map(_.asTenantRole) ++ members.filterNot(_.isUser).map(_.asTenantRole)

  ApiValidator.runDuplicatesDetector("Case team", "user", users.map(_.userId))
  ApiValidator.runDuplicatesDetector("Case team", "tenant role", tenantRoles.map(_.tenantRole))

  def asTeam: CaseTeam = CaseTeam(users = getUsers, tenantRoles = getRoles)
}
