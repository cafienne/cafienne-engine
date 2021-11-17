package org.cafienne.identity

import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.service.db.record.{CaseTeamUserRecord, UserRoleRecord}

object TestIdentityFactory {

  def createTenantUser(id: String, tenant: String = "", name: String = "", roles: Set[String] = Set(), email: String = "") : TenantUser = {
    TenantUser(id, roles, tenant, name = id, email = email, enabled = true)
  }

  def createPlatformUser(userId: String, tenant: String, roles: Set[String]) : PlatformUser = {
    PlatformUser(userId, Seq(TenantUser(userId, roles, tenant, name = "", email = "")))
  }

  def createTeamMember(caseId: String, tenant: String, user: PlatformUser, caseRole: String): CaseTeamUserRecord = {
    CaseTeamUserRecord(caseInstanceId = caseId, tenant = tenant, userId = user.id, caseRole = caseRole, origin = "tenant", isOwner = true)
  }

  def asDatabaseRecords(user: TenantUser) : Seq[UserRoleRecord] = {
    var result:Seq[UserRoleRecord] = Seq()
    user.roles.foreach(role => result = result :+ UserRoleRecord(user.id, user.tenant, user.name, user.email, role, isOwner = false, enabled = true))
    result
  }

  def asDatabaseRecords(user: PlatformUser) : Seq[UserRoleRecord] = {
    var result:Seq[UserRoleRecord] = Seq()
    user.users.foreach(tenantUser => result = result ++ asDatabaseRecords(tenantUser))
    result
  }

  def asDatabaseRecords(users: Seq[PlatformUser]) : Seq[UserRoleRecord] = {
    var result:Seq[UserRoleRecord] = Seq()
    users.foreach(user => result = result ++ asDatabaseRecords(user))
    result
  }
}
