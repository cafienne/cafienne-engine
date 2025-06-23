package org.cafienne.persistence.querydb.query.tenant.implementation

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity._
import org.cafienne.persistence.querydb.query.QueryDBReader
import org.cafienne.persistence.querydb.query.exception._
import org.cafienne.persistence.querydb.query.tenant.TenantQueries
import org.cafienne.persistence.querydb.record.{TenantRecord, UserRoleRecord}
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class TenantQueriesImpl(val queryDB: QueryDB) extends QueryDBReader with TenantQueries with LazyLogging {
  val dbConfig = queryDB.dbConfig

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

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

  override def getPlatformUser(userId: String): Future[PlatformUser] = {
    val tenantUsersQuery = TableQuery[UserRoleTable].filter(_.userId === userId).filter(_.enabled === true)
    val consentGroupsQuery = TableQuery[ConsentGroupMemberTable].filter(_.userId === userId)

    val dbRetrieval = for {
      tenantRecords <- db.run(tenantUsersQuery.result)
      groupRecords <- db.run(consentGroupsQuery.result)
    } yield (tenantRecords, groupRecords)

    dbRetrieval.map(records => {
      val tenantRecords = records._1.filter(_.userId == userId)
      val groupRecords = records._2.filter(_.userId == userId)

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
    })
  }

  override def determineOriginOfUsers(users: Seq[String], tenant: String): Future[Seq[(String, Origin)]] = {
    val tenantMembership = TableQuery[UserRoleTable].filter(_.userId.inSet(users)).filter(_.tenant === tenant).filter(_.role_name === "").map(_.userId).distinct.take(users.length)
    val platformRegistration = TableQuery[UserRoleTable].filter(_.userId.inSet(users)).filterNot(_.tenant === tenant).filter(_.role_name === "").map(_.userId).distinct.take(users.length)
    val query = tenantMembership.joinFull(platformRegistration)
    db.run(query.result).map(records => {
      val tenantUserIds = records.filter(_._1.isDefined).map(_._1.get).toSet
      val platformUserIds = records.filter(_._2.isDefined).map(_._2.get).toSet

      def determineOrigin(userId: String) = {
        if (tenantUserIds.contains(userId)) Origin.Tenant
        else if (platformUserIds.contains(userId)) Origin.Platform
        else Origin.IDP
      }

      users.map(user => (user, determineOrigin(user)))
    })
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

  override def getTenantGroupsUsage(user: TenantUser, tenant: String): Future[Map[String, mutable.HashMap[String, ListBuffer[String]]]] = {
    val query = for {
      tenantGroups <- TableQuery[ConsentGroupTable].filter(_.tenant === tenant)
      tenantsUsingGroup <- TableQuery[CaseInstanceTeamGroupTable]
        .filter(_.tenant =!= tenant)
        .filter(_.groupId === tenantGroups.id)
    } yield (tenantsUsingGroup)

    db.run(query.result).map(records => {
      val groups: mutable.HashMap[String, mutable.HashMap[String, ListBuffer[String]]] = mutable.HashMap[String, mutable.HashMap[String, ListBuffer[String]]]()
      records.foreach(record => {
        val group: mutable.HashMap[String, ListBuffer[String]] = groups.getOrElseUpdate(record.groupId, mutable.HashMap[String, ListBuffer[String]]())
        val tenant: ListBuffer[String] = group.getOrElseUpdate(record.tenant, ListBuffer[String]())
        tenant += record.caseInstanceId
      })
      groups.toMap

    })

    /*
    override def getTenantGroupsUsage(user: TenantUser, tenant: String): Future[Map[String, mutable.Seq[String]]] = {
      val query = TableQuery[ConsentGroupTable].filter(_.tenant === tenant)
          .join(TableQuery[CaseInstanceTeamGroupTable].filter(_.tenant =!= tenant)).on(_.id === _.groupId)

      db.run(query.result).map(records => {
        println(s"Found ${records.size} records: ")
        val groups = mutable.Map[String, mutable.Seq[String]]()
        records.foreach(record => groups.getOrElseUpdate(record._1.id, mutable.Seq[String]()) ++ record._2.tenant)
        groups
      })
    }

      *
      */

  }
}
