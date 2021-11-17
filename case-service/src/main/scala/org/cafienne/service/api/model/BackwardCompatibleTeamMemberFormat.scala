package org.cafienne.service.api.model

import org.cafienne.actormodel.identity.Origin
import org.cafienne.cmmn.actorapi.command.team.{CaseTeamTenantRole, CaseTeamUser, UpsertMemberData}

case class BackwardCompatibleTeamMemberFormat(user: Option[String], // Old property, to be ccompatiblty
                                              roles: Option[Set[String]], // Old property, just keep it here to remain compatible
                                              // New structure below
                                              memberId: Option[String],
                                              memberType: Option[String],
                                              removeRoles: Option[Set[String]],
                                              caseRoles: Option[Set[String]],
                                              isOwner: Option[Boolean]) {
  lazy val isUser: Boolean = memberType.getOrElse("user") != "role"
  private lazy val getRoles = caseRoles.getOrElse(roles.getOrElse(Set()))
  private lazy val identifier = memberId.getOrElse(user.getOrElse(throw new IllegalArgumentException("Member id is missing; consider migrating to the new case team format")))
  private lazy val ownership = {
    if (isOwner.nonEmpty) isOwner.get // If the value of owner is filled, then that precedes (both in old and new format)
    else if (user.nonEmpty) true // Old format ==> all users become owner
    else false // New format, take what is set
  }

  def upsertMemberData: UpsertMemberData = {
    new UpsertMemberData(id = identifier, isUser = isUser, caseRoles = getRoles, removeRoles = removeRoles.getOrElse(Set()), ownership = isOwner)
  }

  def asTenantRole = new CaseTeamTenantRole(tenantRoleName = identifier, caseRoles = getRoles, isOwner = ownership)

  // Note: this no longer takes removeRoles into account, as the new user will replace existing user
  //  if this leads to issues with someone, then we can provide a fix
  def asUser: CaseTeamUser = CaseTeamUser.from(userId = identifier, origin = Origin.IDP, caseRoles = getRoles, isOwner = ownership)
}
