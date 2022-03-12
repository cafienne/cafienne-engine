package org.cafienne.querydb.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{ConsentGroupMembership, PlatformUser, TenantUser, UserIdentity}
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}
import org.cafienne.querydb.query.exception.{ConsentGroupMemberSearchFailure, ConsentGroupSearchFailure, TenantSearchFailure, TenantUserSearchFailure, UserSearchFailure}
import org.cafienne.querydb.record.{ConsentGroupMemberRecord, TenantRecord, UserRoleRecord}
import org.cafienne.querydb.schema.table.{ConsentGroupTables, TenantTables}

import scala.concurrent.{ExecutionContext, Future}

trait UserQueries {
  def getTenant(tenantId: String): Future[TenantRecord] = ???

  def getTenantUser(user: UserIdentity, tenant: String): Future[TenantUser] = ???

  def getPlatformUser(userId: String): Future[PlatformUser] = ???

  def getPlatformUsers(users: Seq[String]): Future[Seq[PlatformUser]] = ???

  def getTenantUsers(tenantUser: TenantUser): Future[Seq[TenantUser]] = ???

  def getDisabledTenantUserAccounts(tenantUser: TenantUser): Future[Seq[TenantUser]] = ???

  def getTenantUser(tenantUser: TenantUser, userId: String): Future[TenantUser] = ???

  def getConsentGroup(user: UserIdentity, groupId: String): Future[ConsentGroup] = ???

  def getConsentGroups(groupIds: Seq[String]): Future[Seq[ConsentGroup]] = ???

  def getConsentGroupMember(user: UserIdentity, groupId: String, userId: String): Future[ConsentGroupMember] = ???

  def authorizeConsentGroupMembershipAndReturnTenant(user: UserIdentity, groupId: String): Future[String] = ???
}


class TenantQueriesImpl extends UserQueries with LazyLogging
  with TenantTables
  with ConsentGroupTables {

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val rolesQuery = TableQuery[UserRoleTable]

  override def getTenant(tenantId: String): Future[TenantRecord] = {
    db.run(TableQuery[TenantTable].filter(_.name === tenantId).filter(_.enabled).result.headOption).map {
      case None => throw TenantSearchFailure(tenantId)
      case Some(record) => record
    }
  }

  override def getTenantUser(user: UserIdentity, tenant: String): Future[TenantUser] = {
    val query = TableQuery[UserRoleTable].filter(_.userId === user.id).filter(_.tenant === tenant)
    db.run(query.result).map(records => {
      // First find the user record itself. Throw an exception if that is not found. This is also done when records list is empty.
      val userRecord = records.find(record => record.role_name.isBlank).fold(throw TenantUserSearchFailure(tenant, user.id))(r => r)
      val roles = records.filter(record => !record.role_name.isBlank && record.enabled).map(_.role_name).toSet
      createTenantUser(userRecord, roles)
    })
  }

  override def getPlatformUser(userId: String): Future[PlatformUser] = getPlatformUsers(Seq(userId)).map(_.head)

  override def getPlatformUsers(users: Seq[String]): Future[Seq[PlatformUser]] = {
    val tenantUsersQuery = TableQuery[UserRoleTable].filter(_.userId.inSet(users)).filter(_.enabled === true)
    val consentGroupsQuery = TableQuery[ConsentGroupMemberTable].filter(_.userId.inSet(users))

    val usersQuery = tenantUsersQuery.joinFull(consentGroupsQuery).on((left, right) => left.userId === right.userId)

    db.run(usersQuery.result).map(records => {
      users.map(userId => {
        val tenantRecords = records.filter(_._1.nonEmpty).map(_._1.get).filter(_.userId == userId)
        val groupRecords = records.filter(_._2.nonEmpty).map(_._2.get).filter(_.userId == userId)
        createPlatformUser(userId, tenantRecords, groupRecords)
      })
    })
  }

  private def createPlatformUser(userId: String, tenantRecords: Seq[UserRoleRecord], groupRecords: Seq[ConsentGroupMemberRecord]): PlatformUser = {
    //    println(s"User $userId has ${tenantRecords.length} tenant roles and ${groupRecords.length} group roles")
    val tenantUsers = tenantRecords.map(_.tenant).toSet[String].map(tenant => {
      val userRecords = tenantRecords.filter(_.tenant == tenant)
      val roles = userRecords.filterNot(_.role_name.isBlank).map(_.role_name).toSet
      val user = userRecords.find(_.role_name.isBlank)
      if (user.isEmpty) None // as we filter on 'enabled===true', it can happen that a disabled user account is fetched. This means user is not active in that tenant, and it should not return
      else Some(createTenantUser(user.get, roles))
    }).filter(_.nonEmpty).map(_.get).toSeq

    val groups: Seq[ConsentGroupMembership] = groupRecords.filter(_.role.isBlank).map(_.group).toSet[String].map(groupId => {
      val groupInfo = groupRecords.filter(_.group == groupId)
      val roles = groupInfo.map(_.role).toSet
      val isOwner: Boolean = groupInfo.exists(_.isOwner)
      ConsentGroupMembership(groupId, roles = roles, isOwner = isOwner)
    }).toSeq

    //    println(s"User $userId is member of ${tenantUsers.length} tenants ${tenantUsers.map(_.tenant)} and ${groups.length} groups ${groups.map(_.groupId)}\n")
    PlatformUser(userId, tenantUsers, groups)
  }

  override def getTenantUsers(user: TenantUser): Future[Seq[TenantUser]] = {
    readAllTenantUsers(user).map(p => p.filter(t => t.enabled))
  }

  private def readAllTenantUsers(user: TenantUser): Future[Seq[TenantUser]] = {
//    // First a security check
//    platformUser.shouldBelongTo(tenant)

    val users = TableQuery[UserRoleTable].filter(_.tenant === user.tenant)
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

  private def createTenantUser(user: UserRoleRecord, roles: Set[String]): TenantUser = {
    TenantUser(user.userId, tenant = user.tenant, roles = roles, isOwner = user.isOwner, name = user.name, email = user.email, enabled = user.enabled)
  }

  override def getDisabledTenantUserAccounts(tenantUser: TenantUser): Future[Seq[TenantUser]] = {
    readAllTenantUsers(tenantUser).map(p => p.filterNot(t => t.enabled))
  }

  // Note: this also returns a user if the account for that user has been disabled
  override def getTenantUser(tenantUser: TenantUser, userId: String): Future[TenantUser] = {
    val users = TableQuery[UserRoleTable].filter(_.tenant === tenantUser.tenant).filter(_.userId === userId)
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

  override def getConsentGroups(groupIds: Seq[String]): Future[Seq[ConsentGroup]] = {
    val query = TableQuery[ConsentGroupTable].filter(_.id.inSet(groupIds))
      .join(TableQuery[ConsentGroupMemberTable]).on(_.id === _.group)

    db.run(query.result).map(records => {
      val groups = records.map(_._1).map(g => (g.id, g.tenant)).toSet
      val members = records.map(_._2).filter(_.role.isBlank)
      val roles = records.map(_._2).filterNot(_.role.isBlank)
      groups.map(group => {
        val groupUsers = members.filter(_.group == group._1)
        val groupMembers = groupUsers.map(member => {
          val memberRoles = roles.filter(_.group == group._1).filter(_.userId == member.userId).map(_.role)
          new ConsentGroupMember(member.userId, memberRoles.toSet, member.isOwner)
        })
        new ConsentGroup(group._1, group._2, groupMembers)
      }).toSeq
    })
  }

  override def getConsentGroup(user: UserIdentity, groupId: String): Future[ConsentGroup] = {
    val consentGroupQuery = for {
      groupQuery <- TableQuery[ConsentGroupTable].filter(_.id === groupId)
      _ <- consentGroupMembershipQuery(user, groupQuery.id) // User must be member
    } yield groupQuery

    val queries = for {
      group <- db.run(consentGroupQuery.result)
      members <- db.run(TableQuery[ConsentGroupMemberTable].filter(_.group === groupId).result)
    } yield (group, members)

    queries.map(result => {
      if (result._1.isEmpty) {
        throw ConsentGroupSearchFailure(groupId)
      }
      val group = result._1.head
      val id = group.id
      val tenant = group.tenant

      val members = result._2
      val users = members.filter(_.role.isBlank)
      val memberList: Seq[ConsentGroupMember] = users.map(user => {
        val userRoles = members.filter(_.userId == user.userId).filterNot(_.role.isBlank).map(_.role)
        ConsentGroupMember(user.userId, userRoles, user.isOwner)
      })
      ConsentGroup(id = id, tenant = tenant, members = memberList)
    })
  }

  private def consentGroupMembershipQuery(user: UserIdentity, groupId: Rep[String]): Query[ConsentGroupMemberTable, ConsentGroupMemberRecord, Seq] = {
    TableQuery[ConsentGroupMemberTable].filter(_.group === groupId).filter(_.userId === user.id).filter(_.role === "")
  }

  override def getConsentGroupMember(user: UserIdentity, groupId: String, userId: String): Future[ConsentGroupMember] = {
    // Pay attention: This query filters both on the requested and requesting user; one used for authorization of the requesting user.
    val query = TableQuery[ConsentGroupMemberTable]
      .filter(_.group === groupId)
      .filter(member => member.userId === userId // Get all requested records
        || (member.userId === user.id && member.role === "")) // And the requestor record with blank role.

    db.run(query.result).map { records =>
      // First check that the requestor is a group member
      if (!records.exists(_.userId == user.id)) {
        // The user does not have access to this group, so can also not ask for member information
        throw ConsentGroupSearchFailure(groupId)
      }

      // Now create the group member information.
      val groupMemberRecords = records.filter(_.userId == userId)
      val userRecords = groupMemberRecords.filter(_.role.isEmpty)
      if (userRecords.isEmpty) {
        throw ConsentGroupMemberSearchFailure(userId)
      }
      val roles = groupMemberRecords.filterNot(_.role.isEmpty).map(_.role)
      ConsentGroupMember(userRecords.head.userId, roles, userRecords.head.isOwner)
    }
  }

  override def authorizeConsentGroupMembershipAndReturnTenant(user: UserIdentity, groupId: String): Future[String] = {
    val consentGroupQuery = for {
      groupQuery <- TableQuery[ConsentGroupTable].filter(_.id === groupId)
      _ <- consentGroupMembershipQuery(user, groupQuery.id) // User must be member
    } yield groupQuery

    db.run(consentGroupQuery.result.headOption).map {
      case Some(group) => group.tenant
      case None => throw ConsentGroupSearchFailure(groupId)
    }
  }
}
