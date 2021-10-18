package org.cafienne.identity

import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.service.db.record.{CaseTeamMemberRecord, UserRoleRecord}

object TestIdentityFactory {

  def createTenantUser(id: String, tenant: String = "", name: String = "", roles: Set[String] = Set(), email: String = "") : TenantUser = {
    TenantUser(id, roles, tenant, name = id, email = email, enabled = true)
  }

  def createPlatformUser(userId: String, tenant: String, roles: Set[String]) : PlatformUser = {
    PlatformUser(userId, Seq(TenantUser(userId, roles, tenant, name = "", email = "")))
  }

  def createTeamMember(caseId: String, tenant: String, user: PlatformUser, caseRole: String): CaseTeamMemberRecord = {
    CaseTeamMemberRecord(caseInstanceId = caseId, tenant = tenant, memberId = user.id, caseRole = caseRole, isTenantUser = true, isOwner = true, active = true)
  }

  def asDatabaseRecords(user: TenantUser) : Seq[UserRoleRecord] = {
    var result:Seq[UserRoleRecord] = Seq()
    user.roles.map(role => result = result :+ UserRoleRecord(user.id, user.tenant, user.name, user.email, role, false, true))
    result
  }

  def asDatabaseRecords(user: PlatformUser) : Seq[UserRoleRecord] = {
    var result:Seq[UserRoleRecord] = Seq()
    user.users.map(tenantUser => result = result ++ asDatabaseRecords(tenantUser))
    result
  }

  def asDatabaseRecords(users: Seq[PlatformUser]) : Seq[UserRoleRecord] = {
    var result:Seq[UserRoleRecord] = Seq()
    users.map(user => result = result ++ asDatabaseRecords(user))
    result
  }
}
