package org.cafienne.cmmn.test

import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}
import org.cafienne.cmmn.actorapi.command.team.CaseTeamUser

class TestUser(userId: String, userRoles: Array[String]) extends CaseUserIdentity {
  override val id: String = userId
  override val origin: Origin = Origin.Tenant
  override val tenantRoles: Set[String] = userRoles.toSet

  def asCaseMember: CaseTeamUser = CaseTeamUser.from(id, origin, tenantRoles)

  def asCaseOwner: CaseTeamUser = CaseTeamUser.from(id, origin, tenantRoles, isOwner = true)
}