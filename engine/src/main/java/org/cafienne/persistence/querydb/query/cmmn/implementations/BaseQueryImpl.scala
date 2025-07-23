/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.query.cmmn.implementations

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{ConsentGroupMembership, Origin, UserIdentity}
import org.cafienne.engine.actorapi.CaseFamily
import org.cafienne.persistence.querydb.query.cmmn.authorization.CaseMembership
import org.cafienne.persistence.querydb.query.cmmn.implementations.basequeries.{IdentifierFilterQuery, TaskAccessHelper, TenantRegistrationQueries}
import org.cafienne.persistence.querydb.record._
import org.cafienne.persistence.querydb.schema.QueryDB
import org.cafienne.persistence.querydb.schema.table.CaseIdentifierRecord

import scala.concurrent.{ExecutionContext, Future}

class BaseQueryImpl(val queryDB: QueryDB) extends TenantRegistrationQueries with IdentifierFilterQuery with TaskAccessHelper with LazyLogging {

  val dbConfig = queryDB.dbConfig
  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val caseInstanceQuery = TableQuery[CaseInstanceTable]
  val caseDefinitionQuery = TableQuery[CaseInstanceDefinitionTable]
  val caseFileQuery = TableQuery[CaseFileTable]
  val caseIdentifiersQuery = TableQuery[CaseBusinessIdentifierTable]

  val planItemTableQuery = TableQuery[PlanItemTable]

  def getCaseMembership(caseInstanceId:String, user: UserIdentity, exception: String => Exception, msg: String): Future[CaseMembership] = {
//    if (msg == caseInstanceId) {
//      println(s"Trying to fetch case '$caseInstanceId' ")
//    } else {
//      println(s"Trying to fetch case '$caseInstanceId' for task '$msg'")
//    }

    def fail: String = throw exception(msg) // Fail when either caseId or tenantId remains to be empty

    val originQuery = {
      val tenantUser =
        TableQuery[CaseInstanceTable]
          .filter(_.id === caseInstanceId)
          .joinLeft(TableQuery[UserRoleTable]
            .filter(_.userId === user.id)
            .filter(_.role_name === "")
            .map(user => (user.userId, user.tenant)))
          .on(_.tenant === _._2).map(join => (join._1.id, join._1.tenant, join._2, join._1.rootCaseId))
      val platformUser = TableQuery[UserRoleTable].filter(_.userId === user.id).filter(_.role_name === "").map(_.userId).take(1)

      tenantUser.joinFull(platformUser)
    }

    val groupMembership = TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId)
      .join(TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id))
      .on((caseGroup, group) => caseGroup.groupId === group.group && (caseGroup.groupRole === group.role || group.isOwner))
      .map(_._2).map(group => (group.group, group.isOwner, group.role)) // Note: we need GROUP ownership, not case team ownership!!!

    val tenantRoleBasedMembership = tenantRoleBasedMembershipQuery(caseInstanceId, user).map(_.tenantRole)

    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === user.id)
      .filter(_.caseInstanceId === caseInstanceId)
      .map(_.userId)

    val userRoleBasedMembership = TableQuery[CaseInstanceTeamTenantRoleTable]
      .filter(_.caseInstanceId === caseInstanceId)
      .filter(_.tenantRole.inSet(user.roles))
      .map(_.tenantRole)

    val query =
      originQuery.joinLeft(
        // Note: order matters. Putting group membership at the end generates an invalid SQL statement
        //  guess that's some kind of issue in Slick
        groupMembership.joinFull(tenantRoleBasedMembership.joinFull(userIdBasedMembership.joinFull(userRoleBasedMembership))))

//    println("CASE MEMBERSHIP QUERY:\n\n" + query.result.statements.mkString("\n")+"\n\n")
    val records = db.run(query.distinct.result)

    records.map(x => {
      if (x.isEmpty) {
//        println(" Failing because there records are not found")
        fail
      }

      val originRecords = x.map(_._1) //filter(_.isDefined).map(_.get)
      if (originRecords.headOption.isEmpty) {
//        println(" Failing because head option is empty")
        fail // Case does not exist
      }

      if (originRecords.head._1.isEmpty) {
//        println(" Failing because head._1 is empty")
        fail // Again, case apparently does not exist (then why do we have a head in the first place ??? Perhaps it is filled with all NULL values???
      }

      val caseFamily = CaseFamily(originRecords.head._1.get._4)
      val caseId = originRecords.head._1.get._1
      val tenantId = originRecords.head._1.get._2
//      println(" Case id: " + caseId)
//      println(" Tenant id: " + tenantId)
      val origin = {
        if (originRecords.isEmpty) Origin.IDP // No platform registration for this user id
        else if (originRecords.head._1.get._3.isDefined) Origin.Tenant
        else if (originRecords.head._2.isDefined) Origin.Platform
        else Origin.IDP // Just a default, should not reach this statement at all.
      }
//      println(s" User ${user.id} has origin $origin")



      val membershipRecords: Seq[(Option[(String, Boolean, String)], Option[(Option[String], Option[(Option[String], Option[String])])])] = x.map(_._2).filter(_.isDefined).map(_.get)
      val userAndRoleRecords = membershipRecords.map(_._2).filter(_.isDefined).map(_.get)
      val userBasedMembership: Set[(Option[String], Option[String])] = userAndRoleRecords.map(_._2).filter(_.isDefined).map(_.get).toSet
      val userIdBasedMembership: Set[String] = userBasedMembership.filter(_._1.isDefined).map(_._1.get)
      val userRoleBasedMembership: Set[String] = userBasedMembership.filter(_._2.isDefined).map(_._2.get)
//      println(s"Found ${userRecords.size} user records")

      val userTenantRoles: Set[String] = userAndRoleRecords.map(_._1).filter(_.isDefined).map(_.get).toSet ++ userRoleBasedMembership
//      println(s"Found ${tenantRoleRecords.size} tenant role records")
      val groupRecords: Set[ConsentGroupMemberRecord] = membershipRecords.map(_._1).filter(_.isDefined).map(_.get).map(group => ConsentGroupMemberRecord(group = group._1, userId = user.id, isOwner = group._2, role = group._3)).toSet
//      println(s"Found ${groupRecords.size} group records")

      val groups = groupRecords.map(_.group)
      val groupBasedMembership: Seq[ConsentGroupMembership] = groups.map(groupId => {
        val groupElements = groupRecords.filter(_.group == groupId)
        val isOwner = groupElements.exists(_.isOwner)
        val roles = groupElements.map(_.role)
        ConsentGroupMembership(groupId, roles, isOwner)
      }).toSeq

      // ... and, if those are non empty only then we have an actual access to this case
      if (userIdBasedMembership.isEmpty && groupBasedMembership.isEmpty && userTenantRoles.isEmpty) {
        // All rows empty, no access to this case
//        println("Failing because membership sets are empty")
        fail
      }

      new CaseMembership(id = user.id, origin = origin, tenantRoles = userTenantRoles, groups = groupBasedMembership, caseInstanceId = caseId, tenant = tenantId, caseFamily = caseFamily)

    })
  }

  def tenantRoleBasedMembershipQuery(caseInstanceId: Rep[String], user: UserIdentity): Query[CaseInstanceTeamTenantRoleTable, CaseTeamTenantRoleRecord, Seq] = {
    queryAllRolesInAllTenantsForUser(user)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseInstanceId))
      // The tenant role must be in the case team, and also the user must have the role in the same tenant
      .on((left, right) => (left._1 === right.tenantRole) && left._2 === right.tenant)
      .map(_._2)
  }

  /**
    * Query that validates that the user belongs to the team of the specified case, either by explicit
    * membership of the user id, or by one of the tenant roles of the user that are bound to the team of the case
    */
  def membershipQuery(user: UserIdentity, caseInstanceId: Rep[String]): Query[CaseIdentifierView, CaseIdentifierRecord, Seq] = {
    val groupMembership = TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId))
      .on((group, member) => {
        // User belongs to the case team if the group belongs to the case team and either:
        // - the user has a group role matching the case membership's group role
        // - or the user is group owner
        group.group === member.groupId && (group.role === member.groupRole || group.isOwner)
      })
      .map(_._2.caseInstanceId)

    val tenantRoleBasedMembership = tenantRoleBasedMembershipQuery(caseInstanceId, user).map(_.caseInstanceId)

    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.caseInstanceId === caseInstanceId)
      .filter(_.userId === user.id)
      .map(_.caseInstanceId)

    val userIdentityRoleBasedMembership = TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseInstanceId).filter(_.tenantRole.inSet(user.roles))

    // Return a filter on the case that also matches membership existence somewhere
    TableQuery[CaseIdentifierView]
      .filter(_.id === caseInstanceId)
      .filter(_ => userIdBasedMembership.exists || tenantRoleBasedMembership.exists || groupMembership.exists || userIdentityRoleBasedMembership.exists)
  }

  /**
    * Query that validates that the user belongs to the team of the specified case,
    * and adds an optional business identifiers filter to the query.
    */
  def membershipQuery(user: UserIdentity, caseInstanceId: Rep[String], identifiers: Option[String]): Query[CaseIdentifierView, CaseIdentifierRecord, Seq] = {
    if (identifiers.isEmpty) membershipQuery(user, caseInstanceId)
    else for {
      teamMemberShip <- membershipQuery(user, caseInstanceId)
      _ <- new BusinessIdentifierFilterParser(identifiers).asQuery(caseInstanceId)
    } yield teamMemberShip
  }
}
