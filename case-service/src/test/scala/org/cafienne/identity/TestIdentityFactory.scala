package org.cafienne.identity

import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.service.api.tenant.UserRole

object TestIdentityFactory {

  def createTenantUser(id: String, tenant: String = "", name: String = "", roles: List[String] = List.empty[String], email: String = "") : TenantUser = {
    TenantUser(id, roles, tenant, id, email, true)
  }

  def createPlatformUser(userId: String, tenant: String, roles: Seq[String]) : PlatformUser = {
    PlatformUser(userId, Seq(TenantUser(userId, roles, tenant, "", "")))
  }

  def asDatabaseRecords(user: TenantUser) : Seq[UserRole] = {
    var result:Seq[UserRole] = Seq()
    user.roles.map(role => result = result :+ UserRole(user.id, user.tenant, user.name, user.email, role))
    result
  }

  def asDatabaseRecords(user: PlatformUser) : Seq[UserRole] = {
    var result:Seq[UserRole] = Seq()
    user.users.map(tenantUser => result = result ++ asDatabaseRecords(tenantUser))
    result
  }

  def asDatabaseRecords(users: Seq[PlatformUser]) : Seq[UserRole] = {
    var result:Seq[UserRole] = Seq()
    users.map(user => result = result ++ asDatabaseRecords(user))
    result
  }
}
