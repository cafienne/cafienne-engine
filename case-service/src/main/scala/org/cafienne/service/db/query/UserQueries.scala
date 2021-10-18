package org.cafienne.service.db.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.service.db.query.exception.UserSearchFailure
import org.cafienne.service.db.record.UserRoleRecord
import org.cafienne.service.db.schema.table.TenantTables

import scala.concurrent.{ExecutionContext, Future}

trait UserQueries {
  def getPlatformUser(user: AuthenticatedUser): Future[PlatformUser] = ???

  def getSelectedTenantUsers(tenant: String, users: Seq[String]): Future[Seq[TenantUser]] = ???

  def getTenantUsers(platformUser: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getDisabledTenantUsers(platformUser: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getTenantUser(platformUser: PlatformUser, tenant: String, userId: String): Future[TenantUser] = ???
}


class TenantQueriesImpl extends UserQueries with LazyLogging
  with TenantTables {

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val rolesQuery = TableQuery[UserRoleTable]

  override def getPlatformUser(user: AuthenticatedUser): Future[PlatformUser] = {
    val query = TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.enabled === true)

    db.run(query.result).map(records => {
      val users = records.filter(record => record.role_name == "")
      val tenants = users.map(user => user.tenant)
      val tenantUsers = tenants.map(tenant => {
        val user = users.find(u => u.tenant == tenant).get// no worries, this always exists (obviously, otherwise there would not be a tenant).
        val roles = records.filter(record => record.tenant == tenant && !record.role_name.isBlank)
        createTenantUser(user, roles)
      })
      PlatformUser(user.userId, tenantUsers)
    })
  }

  override def getSelectedTenantUsers(tenant: String, users: Seq[String]): Future[Seq[TenantUser]] = {
    val query = TableQuery[UserRoleTable].filter(_.userId.inSet(users)).filter(_.tenant === tenant).filter(_.enabled === true)
    db.run(query.result).map(records => {
      val users = records.filter(record => record.role_name == "")
      users.map(user => {
        val roles = records.filter(record => record.userId == user.userId && !record.role_name.isBlank)
        createTenantUser(user, roles)
      })
    })
  }

  private def readAllTenantUsers(platformUser: PlatformUser, tenant: String) = {
    // First a security check
    platformUser.shouldBelongTo(tenant)

    val users = TableQuery[UserRoleTable].filter(_.tenant === tenant)
    db.run(users.result).map(records => {
      // First sort and store all roles by user-id
      val userRecords = records.filter(record => record.role_name.isBlank)
      val roleRecords = records.filter(record => !record.role_name.isBlank && record.enabled)

      val users = userRecords.map(user => {
        val roles = roleRecords.filter(role => role.userId == user.userId)
        createTenantUser(user, roles)
      })
      users
    })
  }

  override def getTenantUsers(platformUser: PlatformUser, tenant: String): Future[Seq[TenantUser]] = {
    readAllTenantUsers(platformUser, tenant).map(p => p.filter(t => t.enabled))
  }

  override def getDisabledTenantUsers(platformUser: PlatformUser, tenant: String): Future[Seq[TenantUser]] = {
    readAllTenantUsers(platformUser, tenant).map(p => p.filterNot(t => t.enabled))
  }

  // Note: this also returns a user if the account for that user has been disabled
  override def getTenantUser(platformUser: PlatformUser, tenant: String, userId: String): Future[TenantUser] = {
    // First a security check
    platformUser.shouldBelongTo(tenant)
    val users = TableQuery[UserRoleTable].filter(_.tenant === tenant).filter(_.userId === userId)
    db.run(users.result).map(roleRecords => {
      // Filter out user
      val user = roleRecords.find(role => role.role_name.isBlank).getOrElse({
        throw UserSearchFailure(userId)
      })
      // Filter out names of enabled roles
      val roles = roleRecords.filter(role => role.enabled).filter(role => !role.role_name.isBlank)
      createTenantUser(user, roles)
    })
  }

  private def createTenantUser(user: UserRoleRecord, roles: Seq[UserRoleRecord]): TenantUser = {
    TenantUser(user.userId, roles.map(role => role.role_name).toSet, user.tenant, isOwner = user.isOwner, user.name, user.email, enabled = user.enabled)
  }
}
