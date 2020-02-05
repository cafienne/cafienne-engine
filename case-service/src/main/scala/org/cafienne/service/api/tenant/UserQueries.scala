package org.cafienne.service.api.tenant

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}

import scala.collection.mutable
import scala.concurrent.Future

trait UserQueries {
  def getPlatformUser(userId: String) : Future[PlatformUser] = ???

  def getTenantUsers(user: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getTenantUser(user: PlatformUser, tenant: String, userId: String): Future[TenantUser] = ???
}


class TenantQueriesImpl extends UserQueries with LazyLogging
  with TenantTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val rolesQuery = TableQuery[UserRoleTable]

  override def getPlatformUser(userId: String): Future[PlatformUser] = {
    val query = TableQuery[UserRoleTable].filter(_.userId === userId)

    db.run(query.result)
      .map(records => {
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
            val roles = userWithRoles._2.map(role => role.role_name)
            val emptyRole = userWithRoles._2.find(p => p.role_name == "")
            val enabled = emptyRole.map(r => r.enabled).getOrElse({
              logger.warn("UserRole["+user.id+" in tenant "+user.tenant+" has no role with an empty role name, and is therefore disabled")
              false
            })
            TenantUser(user.id, roles, user.tenant, user.name, user.email, enabled)
          })
          new PlatformUser(userId, users)
        }


        from(userId, allUsersWitRoles)
      })
  }

  override def getTenantUsers(user: PlatformUser, tenant: String): Future[Seq[TenantUser]] = {
    // First a security check
    user.shouldBelongTo(tenant)

    val users = TableQuery[UserRoleTable].filter(_.tenant === tenant).filter(_.role_name === "")
    db.run(users.result).map(roleRecords => {
      val users = mutable.Map[String, Seq[UserRole]]()
      roleRecords.map(role => {
        val knownRoles = users.getOrElse(role.userId, Seq())
        users.put(role.userId, knownRoles :+ role)
      })
      val tenantUsers = users.keys.map(userId => {
        val roles = users.getOrElse(userId, Seq())
        val roleNames = roles.map(role => role.role_name)
        val userIdentifyingRole = roles.find(role => role.role_name == "").getOrElse(UserRole(userId, "", tenant, "", "", false))
        TenantUser(userId, roleNames, tenant, userIdentifyingRole.name, userIdentifyingRole.email, userIdentifyingRole.enabled)
      })
      tenantUsers.toSeq
    })
  }

  override def getTenantUser(user: PlatformUser, tenant: String, userId: String): Future[TenantUser] = {
    // First a security check
    user.shouldBelongTo(tenant);
    getPlatformUser(userId).map(u => u.getTenantUser(tenant))
  }
}
