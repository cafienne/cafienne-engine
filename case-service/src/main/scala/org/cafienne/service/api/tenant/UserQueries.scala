package org.cafienne.service.api.tenant

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.service.api.tasks.SearchFailure

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

  override def getPlatformUser(userId: String): Future[PlatformUser] = {
    val query = TableQuery[UserRoleTable].filter(_.userId === userId).filter(_.enabled === true)

    db.run(query.result).map(records => {
        val rolesPerTenant = mutable.Map[String, Seq[UserRole]]()
        val tenantUsers = mutable.Map[String, User]()
        records.map(r => {
          val currentRoles: Seq[UserRole] = rolesPerTenant.get(r.tenant).getOrElse(Seq())
          val role: UserRole = r
          val newRoles = currentRoles :+ role
          rolesPerTenant.put(r.tenant, newRoles)
          tenantUsers.put(r.tenant, User(userId, r.tenant, r.name, r.email, r.enabled))
        })
        val allUsersWitRoles = tenantUsers.keys.map(tenant => (tenantUsers.get(tenant).get, rolesPerTenant.get(tenant).get)).toIndexedSeq

        def from(userId: String, usersWithRoles: Seq[(User, Seq[UserRole])]): PlatformUser = {
          val users = usersWithRoles.map(userWithRoles => {
            val user = userWithRoles._1
            val emptyRole = userWithRoles._2.find(p => p.role_name == "")
            val enabled = emptyRole.map(r => r.enabled).getOrElse({
              logger.warn("UserRole["+user.id+" in tenant "+user.tenant+" has no role with an empty role name, and is therefore disabled")
              false
            })
            val roles = userWithRoles._2.filter(role => role.role_name != "").map(role => role.role_name)
            TenantUser(user.id, roles, user.tenant, user.name, user.email, enabled)
          })
          new PlatformUser(userId, users)
        }

        from(userId, allUsersWitRoles)
      })
  }

  private def readAllTenantUsers(user: PlatformUser, tenant: String) = {
    // First a security check
    user.shouldBelongTo(tenant)

    val users = TableQuery[UserRoleTable].filter(_.tenant === tenant)
    db.run(users.result).map(roleRecords => {
      // First sort and store all roles by user-id
      val users = mutable.Map[String, Seq[UserRole]]()
      roleRecords.map(role => {
        val knownRoles = users.getOrElse(role.userId, Seq())
        users.put(role.userId, knownRoles :+ role)
      })

      // Now go through all the UserRole objects per user-id and map them to TenantUser objects
      val tenantUsers = users.map(entry => {
        val userId = entry._1
        val roles = entry._2
        val roleNames = roles.map(role => role.role_name).filter(roleName => !roleName.trim.isEmpty)
        val userIdentifyingRole = roles.find(role => role.role_name == "").getOrElse(UserRole(userId, "", tenant, "", "", false))
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
      val roleNames = roleRecords.filter(role => role.enabled).map(role => role.role_name).filter(roleName => !roleName.trim.isEmpty)
      // Filter out user
      val userIdentifyingRole = roleRecords.find(role => role.role_name == "").getOrElse({
        throw new SearchFailure(s"User '${userId}' cannot be found")
      })
      TenantUser(userIdentifyingRole.userId, roleNames, tenant, userIdentifyingRole.name, userIdentifyingRole.email, userIdentifyingRole.enabled)
    })
  }
}
