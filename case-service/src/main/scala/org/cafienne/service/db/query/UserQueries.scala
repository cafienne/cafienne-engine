package org.cafienne.service.db.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.service.db.query.exception.UserSearchFailure
import org.cafienne.service.db.record.UserRoleRecord
import org.cafienne.service.db.schema.table.TenantTables

import scala.concurrent.{ExecutionContext, Future}

trait UserQueries {
  def getPlatformUser(userId: String): Future[PlatformUser] = ???

  def getPlatformUsers(users: Seq[String]): Future[Seq[PlatformUser]] = ???

  def getTenantUsers(platformUser: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getDisabledTenantUsers(platformUser: PlatformUser, tenant: String): Future[Seq[TenantUser]] = ???

  def getTenantUser(platformUser: PlatformUser, tenant: String, userId: String): Future[TenantUser] = ???
}


class TenantQueriesImpl extends UserQueries with LazyLogging
  with TenantTables {

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val rolesQuery = TableQuery[UserRoleTable]

  override def getPlatformUser(userId: String): Future[PlatformUser] = getPlatformUsers(Seq(userId)).map(_.head)

  private def createPlatformUser(userId: String, tenantRecords: Seq[UserRoleRecord]): PlatformUser = {
    //    println(s"User $userId has ${tenantRecords.length} tenant roles and ${groupRecords.length} group roles")
    val tenantUsers = tenantRecords.map(_.tenant).toSet[String].map(tenant => {
      val userRecords = tenantRecords.filter(_.tenant == tenant)
      val roles = userRecords.filterNot(_.role_name.isBlank).map(_.role_name).toSet
      val user = userRecords.find(_.role_name.isBlank)
      if (user.isEmpty) None // as we filter on 'enabled===true', it can happen that a disabled user account is fetched. This means user is not active in that tenant, and it should not return
      else Some(createTenantUser(user.get, roles))
    }).filter(_.nonEmpty).map(_.get).toSeq

    //    println(s"User $userId is member of ${tenantUsers.length} tenants ${tenantUsers.map(_.tenant)} and ${groups.length} groups ${groups.map(_.groupId)}\n")
    PlatformUser(userId, tenantUsers)
  }

  override def getPlatformUsers(users: Seq[String]): Future[Seq[PlatformUser]] = {
    val tenantUsersQuery = TableQuery[UserRoleTable].filter(_.userId.inSet(users)).filter(_.enabled === true)

    val usersQuery = tenantUsersQuery

    db.run(usersQuery.result).map(records => {
      users.map(userId => {
        createPlatformUser(userId, records)
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
        val roles = roleRecords.filter(role => role.userId == user.userId).map(_.role_name).toSet
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
      val roles = roleRecords.filter(role => role.enabled).filter(role => !role.role_name.isBlank).map(_.role_name).toSet
      createTenantUser(user, roles)
    })
  }

  private def createTenantUser(user: UserRoleRecord, roles: Set[String]): TenantUser = {
    TenantUser(user.userId, roles, user.tenant, isOwner = user.isOwner, user.name, user.email, enabled = user.enabled)
  }
}
