package org.cafienne.service.api.tenant

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.service.api.projection.UserSearchFailure

import scala.collection.mutable
import scala.concurrent.Future

trait UserQueries {
  def getPlatformUser(userId: String) : Future[PlatformUser] = ???

  def getTenantUsers(user: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getDisabledTenantUsers(user: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getTenantUser(user: PlatformUser, tenant: String, userId: String): Future[TenantUser] = ???
}


class TenantQueriesImpl extends UserQueries with LazyLogging
  with TenantTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val rolesQuery = TableQuery[UserRoleTable]


  final case class User(id: String, tenant: String, name: String, email: String = "", isOwner: Boolean, enabled: Boolean)


  override def getPlatformUser(userId: String): Future[PlatformUser] = {
    val query = TableQuery[UserRoleTable].filter(_.userId === userId).filter(_.enabled === true)

    db.run(query.result).map(records => {
      val users = records.filter(record => record.role_name == "")
      val tenants = users.map(user => user.tenant)
      val tenantUsers = tenants.map(tenant => {
        val user = users.find(u => u.tenant == tenant).get// no worries, this always exists (obviously, otherwise there would not be a tenant).
        val roles = records.filter(record => record.tenant == tenant && !record.role_name.isBlank).map(record => record.role_name)
        TenantUser(user.userId, roles, tenant, user.name, user.email, user.enabled, user.isOwner)
      })
      PlatformUser(userId, tenantUsers)
    })
  }

  private def readAllTenantUsers(user: PlatformUser, tenant: String) = {
    // First a security check
    user.shouldBelongTo(tenant)

    val users = TableQuery[UserRoleTable].filter(_.tenant === tenant)
    db.run(users.result).map(roleRecords => {
      // First sort and store all roles by user-id
      val users = mutable.Map[String, Seq[UserRoleRecord]]()
      roleRecords.map(role => {
        val knownRoles = users.getOrElse(role.userId, Seq())
        users.put(role.userId, knownRoles :+ role)
      })

      // Now go through all the UserRole objects per user-id and map them to TenantUser objects
      val tenantUsers = users.map(entry => {
        val userId = entry._1
        val roles = entry._2
        val roleNames = roles.map(role => role.role_name).filter(roleName => !roleName.isBlank)
        val userIdentifyingRole = roles.find(role => role.role_name == "").getOrElse(UserRoleRecord(userId, "", tenant, "", "", false, false))
        TenantUser(userId, roleNames, tenant, userIdentifyingRole.name, userIdentifyingRole.email, userIdentifyingRole.enabled)
      })
      tenantUsers.toSeq
    })
  }

  override def getTenantUsers(user: PlatformUser, tenant: String): Future[Seq[TenantUser]] = {
    readAllTenantUsers(user, tenant).map(p => p.filter(t => t.enabled))
  }

  override def getDisabledTenantUsers(user: PlatformUser, tenant: String): Future[Seq[TenantUser]] = {
    readAllTenantUsers(user, tenant).map(p => p.filterNot(t => t.enabled))
  }

  // Note: this also returns a user if the account for that user has been disabled
  override def getTenantUser(user: PlatformUser, tenant: String, userId: String): Future[TenantUser] = {
    // First a security check
    user.shouldBelongTo(tenant);
    val users = TableQuery[UserRoleTable].filter(_.tenant === tenant).filter(_.userId === userId)
    db.run(users.result).map(roleRecords => {
      // Filter out names of enabled roles
      val roleNames = roleRecords.filter(role => role.enabled).map(role => role.role_name).filter(roleName => !roleName.isBlank)
      // Filter out user
      val userIdentifyingRole = roleRecords.find(role => role.role_name == "").getOrElse({
        throw UserSearchFailure(userId)
      })
      TenantUser(userIdentifyingRole.userId, roleNames, tenant, userIdentifyingRole.name, userIdentifyingRole.email, userIdentifyingRole.enabled)
    })
  }
}
