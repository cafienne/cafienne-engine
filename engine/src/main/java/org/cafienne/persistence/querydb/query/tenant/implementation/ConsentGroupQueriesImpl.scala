package org.cafienne.persistence.querydb.query.tenant.implementation

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity._
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}
import org.cafienne.persistence.querydb.query.QueryDBReader
import org.cafienne.persistence.querydb.query.exception._
import org.cafienne.persistence.querydb.query.tenant.ConsentGroupQueries
import org.cafienne.persistence.querydb.record.ConsentGroupMemberRecord
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.{ExecutionContext, Future}

class ConsentGroupQueriesImpl(val queryDB: QueryDB) extends QueryDBReader with ConsentGroupQueries with LazyLogging {
  val dbConfig = queryDB.dbConfig

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?


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

  override def getConsentGroupUser(user: UserIdentity, groupId: String): Future[ConsentGroupUser] = {
    val consentGroupQuery = for {
      groupQuery <- TableQuery[ConsentGroupTable].filter(_.id === groupId)
      _ <- consentGroupMembershipQuery(user, groupQuery.id) // User must be member
    } yield groupQuery

    db.run(consentGroupQuery.result.headOption).map {
      case Some(group) => ConsentGroupUser(id = user.id, groupId = group.id, tenant = group.tenant)
      case None => throw ConsentGroupSearchFailure(groupId)
    }
  }
}
