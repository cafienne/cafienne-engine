package org.cafienne.cmmn.test

import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeamMember, MemberKey}

class TestUser(userId: String, userRoles: Array[String]) extends CaseUserIdentity {
  override val id: String = userId
  override val origin: Origin = Origin.Tenant
  override val tenantRoles: Set[String] = userRoles.toSet

  def asCaseMember: CaseTeamMember = CaseTeamMember(MemberKey(id = userId, `type` = "user"), caseRoles = tenantRoles.toSeq, isOwner = Some(false))

  def asCaseOwner: CaseTeamMember = CaseTeamMember(MemberKey(id = userId, `type` = "user"), caseRoles = tenantRoles.toSeq, isOwner = Some(true))
}
