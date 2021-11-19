package org.cafienne.service.api.model

import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamGroup, CaseTeamTenantRole, CaseTeamUser}

case class BackwardCompatibleTeamFormat(
           users: Seq[CaseTeamFormat.CaseTeamUserFormat] = Seq(),
           groups: Seq[CaseTeamFormat.GroupFormat] = Seq(),
           tenantRoles: Seq[CaseTeamFormat.TenantRoleFormat] = Seq(),
           members: Seq[BackwardCompatibleTeamMemberFormat] = Seq()) {
  private lazy val getUsers: Seq[CaseTeamUser] = users.map(_.asCaseTeamUser) ++ members.filter(_.isUser).map(_.asUser)
  private lazy val getGroups: Seq[CaseTeamGroup] = groups.map(_.asGroup)
  private lazy val getRoles: Seq[CaseTeamTenantRole] = tenantRoles.map(_.asTenantRole) ++ members.filterNot(_.isUser).map(_.asTenantRole)

  // Run validations: mappings must be present and users, groups and roles should not contain duplicates
  if (groups.exists(group => group.mappings.isEmpty)) {
    throw new IllegalArgumentException("Case team groups must have one or more mappings defined")
  }

  ApiValidator.runDuplicatesDetector("Case team", "user", users.map(_.userId))
  ApiValidator.runDuplicatesDetector("Case team", "group", groups.map(_.groupId))
  ApiValidator.runDuplicatesDetector("Case team", "tenant role", tenantRoles.map(_.tenantRole))

  def asTeam: CaseTeam = CaseTeam(users = getUsers, groups = getGroups, tenantRoles = getRoles)
}
