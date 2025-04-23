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

package org.cafienne.persistence.querydb.query

import org.cafienne.actormodel.identity.{Origin, UserIdentity}
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.cmmn.definition.CMMNElementDefinition
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.exception.{CaseSearchFailure, PlanItemSearchFailure}
import org.cafienne.persistence.querydb.query.filter.CaseFilter
import org.cafienne.persistence.querydb.query.result.{CaseFileDocumentation, CaseTeamResponse, Documentation, FullCase}
import org.cafienne.persistence.querydb.record._
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.Future

trait CaseQueries {
  def getCaseMembership(caseInstanceId: String, user: UserIdentity): Future[CaseMembership] = ???

  def getCaseOwnership(caseInstanceId: String, user: UserIdentity): Future[CaseOwnership] = ???

  def getFullCaseInstance(caseInstanceId: String, user: UserIdentity): Future[FullCase] = ???

  def getCaseDefinition(caseInstanceId: String, user: UserIdentity): Future[CaseDefinitionRecord] = ???

  def getCaseInstance(caseInstanceId: String, user: UserIdentity): Future[Option[CaseRecord]] = ???

  def getCaseFile(caseInstanceId: String, user: UserIdentity): Future[CaseFileRecord] = ???

  def getCaseFileDocumentation(caseInstanceId: String, user: UserIdentity): Future[CaseFileDocumentation] = ???

  def getCaseTeam(caseInstanceId: String, user: UserIdentity): Future[CaseTeamResponse] = ???

  def getPlanItems(caseInstanceId: String, user: UserIdentity): Future[Seq[PlanItemRecord]] = ???

  def getPlanItem(planItemId: String, user: UserIdentity): Future[PlanItemRecord] = ???

  def getPlanItemDocumentation(planItemId: String, user: UserIdentity): Future[Documentation] = ???

  //  def getCasesStats(user: UserIdentity, tenant: Option[String], from: Int, numOfResults: Int, caseName: Option[String], status: Option[String]): Future[Seq[CaseList]] = ??? // GetCaseList

  def getMyCases(user: UserIdentity, filter: CaseFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[CaseRecord]] = ???

  def getCases(user: UserIdentity, filter: CaseFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[CaseRecord]] = ???
}

class CaseQueriesImpl(queryDB: QueryDB)
  extends BaseQueryImpl(queryDB)
    with CaseQueries {

  import dbConfig.profile.api._

  override def getCaseMembership(caseInstanceId: String, user: UserIdentity): Future[CaseMembership] = {
    super.getCaseMembership(caseInstanceId, user, CaseSearchFailure, caseInstanceId)
  }

  override def getCaseOwnership(caseInstanceId: String, user: UserIdentity): Future[CaseOwnership] = {
    val groupMembership = TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId)
      .join(TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id))
      .on((caseGroup, group) => caseGroup.groupId === group.group && (caseGroup.groupRole === group.role || group.isOwner))
      .map(_._1)

    val tenantRoleBasedMembership = tenantRoleBasedMembershipQuery(caseInstanceId, user)
    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.caseInstanceId === caseInstanceId) // First filter on case id (probably faster than first on user id)
      .filter(_.userId === user.id)

    val query = for {
      groups <- db.run(groupMembership.result)
      roles <- db.run(tenantRoleBasedMembership.result)
      users <- db.run(userIdBasedMembership.result)
    } yield (groups, roles, users)

    query.map(result => {
      val groupMemberships = result._1
      val roleMemberships = result._2
      val userMembership = result._3
      if (groupMemberships.isEmpty && roleMemberships.isEmpty && userMembership.isEmpty) {
        throw CaseSearchFailure(caseInstanceId)
      }

      val isOwner = groupMemberships.exists(_.isOwner) || roleMemberships.exists(_.isOwner) || userMembership.exists(_.isOwner)
      val tenant = (groupMemberships.map(_.tenant) ++ roleMemberships.map(_.tenant) ++ userMembership.map(_.tenant)).toSet.head
      CaseOwnership(user.id, caseInstanceId, tenant, isOwner)
    })
  }

  override def getFullCaseInstance(caseInstanceId: String, user: UserIdentity): Future[FullCase] = {
    val result = for {
      caseInstance <- getCaseInstance(caseInstanceId, user)
      caseTeam <- getCaseTeam(caseInstanceId, user)
      caseFile <- db.run(caseFileQuery.filter(_.caseInstanceId === caseInstanceId).result.headOption)
      casePlan <- db.run(planItemTableQuery.filter(_.caseInstanceId === caseInstanceId).result)
      identifiers <- db.run(caseIdentifiersQuery.filter(_.caseInstanceId === caseInstanceId).filter(_.active === true).result)
    } yield (caseInstance, caseTeam, caseFile, casePlan, identifiers)

    result.map(x => x._1.fold(throw CaseSearchFailure(caseInstanceId))(caseRecord => FullCase(caseRecord, file = x._3, team = x._2, planitems = x._4, identifiers = x._5)))
  }

  override def getCaseInstance(caseInstanceId: String, user: UserIdentity): Future[Option[CaseRecord]] = {
//    println(s"Getting case $caseInstanceId for user ${user.id}")
    val query = for {
      // Get the case
      baseQuery <- caseInstanceQuery.filter(_.id === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery
//        println("QUERY:\n\n" + query.result.statements.mkString("\n")+"\n\n")

    db.run(query.result.headOption)
  }

  override def getCaseTeam(caseInstanceId: String, user: UserIdentity): Future[CaseTeamResponse] = {
    val usersQuery = TableQuery[CaseInstanceTeamUserTable].filter(_.caseInstanceId === caseInstanceId)
    val tenantRolesQuery = TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseInstanceId)
    val groupsQuery = TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId)

    (for {
      userRecords <- db.run(usersQuery.result)
      tenantRoleRecords <- db.run(tenantRolesQuery.result)
      groupRecords <- db.run(groupsQuery.result)
      roleRecords <- db.run(TableQuery[CaseInstanceRoleTable].filter(_.caseInstanceId === caseInstanceId).map(_.roleName).result)
      membership <- db.run(membershipQuery(user, caseInstanceId).result)
    } yield (userRecords, tenantRoleRecords, groupRecords, roleRecords, membership))
      .map(records => {
        if (records._5.isEmpty) {
          throw CaseSearchFailure(caseInstanceId)
        }
        val users = records._1
        val tenantRoles = records._2
        val groups = records._3
        val caseRoleRecords: Seq[String] = records._4

        val team = fillCaseTeam(users, tenantRoles, groups)
        val caseRoles = caseRoleRecords.filterNot(_.isBlank)
        val unassignedRoles = caseRoles.filter(caseRole => !users.exists(_.caseRole == caseRole) && !tenantRoles.exists(_.caseRole == caseRole) && !groups.exists(_.caseRole == caseRole))
        CaseTeamResponse(team, caseRoles = caseRoles, unassignedRoles = unassignedRoles)
      })
  }

  private def fillCaseTeam(userRecords: Seq[CaseTeamUserRecord], tenantRoleRecords: Seq[CaseTeamTenantRoleRecord], groupRecords: Seq[CaseTeamGroupRecord]): CaseTeam = {
    val userMembers = getUserMembers(userRecords)
    val tenantRoleMembers = getTenantRoleMembers(tenantRoleRecords)
    val groupIds: Set[String] = groupRecords.map(_.groupId).toSet
    val groups: Seq[CaseTeamGroup] =groupIds.map(groupId => {
      val mappings = groupRecords.filter(_.groupId == groupId)
      val groupRoles = mappings.map(_.groupRole).toSet
      val groupRoleMappings = groupRoles.map(groupRole => {
        val caseRoles = mappings.filter(_.groupRole == groupRole).map(_.caseRole).toSet.filterNot(_.isBlank)
        val isOwner = mappings.filter(_.groupRole == groupRole).exists(_.isOwner)
        GroupRoleMapping(groupRole = groupRole, isOwner = isOwner, caseRoles = caseRoles)
      })
      CaseTeamGroup(groupId, groupRoleMappings.toSeq)
    }).toSeq
//    val groups: Seq[CaseTeamGroup] = groupIds.map(id => CaseTeamGroup(id, groupRecords.filter(_.groupId == id).map(m => GroupRoleMapping(m.caseRole, m.groupRole, m.isOwner)))).toSeq
    CaseTeam(users = userMembers, tenantRoles = tenantRoleMembers, groups = groups)
  }

  private def getUserMembers(userRecords: Seq[CaseTeamUserRecord]): Seq[CaseTeamUser] = {
    val users = userRecords.filter(record => record.caseRole.isBlank)
    val roles = userRecords.filterNot(record => record.caseRole.isBlank)
    users.map(user => CaseTeamUser.from(userId = user.userId, origin = Origin.getEnum(user.origin), isOwner = user.isOwner, caseRoles = roles.filter(role => role.userId == user.userId).map(_.caseRole).toSet))
  }

  private def getTenantRoleMembers(roleRecords: Seq[CaseTeamTenantRoleRecord]): Seq[CaseTeamTenantRole] = {
    val records = roleRecords.filter(record => record.caseRole.isBlank)
    val caseRoles = roleRecords.filterNot(record => record.caseRole.isBlank)
    records.map(tRole => CaseTeamTenantRole(tenantRoleName = tRole.tenantRole, isOwner = tRole.isOwner, caseRoles = caseRoles.filter(role => role.tenantRole == tRole.tenantRole).map(_.caseRole).toSet))
  }

  override def getCaseDefinition(caseInstanceId: String, user: UserIdentity): Future[CaseDefinitionRecord] = {
    val query = for {
      // Get the case file
      baseQuery <- caseDefinitionQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => result
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getCaseFile(caseInstanceId: String, user: UserIdentity): Future[CaseFileRecord] = {
    val query = for {
      // Get the case file
      baseQuery <- caseFileQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => result
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getCaseFileDocumentation(caseInstanceId: String, user: UserIdentity): Future[CaseFileDocumentation] = {
    val query = for {
      // Get the case file
      baseQuery <- caseDefinitionQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => CaseFileDocumentation(result)
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getPlanItems(caseInstanceId: String, user: UserIdentity): Future[Seq[PlanItemRecord]] = {
    val query = for {
      // Get the items in the case plan
      baseQuery <- planItemTableQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      records
    })
  }

  override def getPlanItem(planItemId: String, user: UserIdentity): Future[PlanItemRecord] = {
    val query = for {
      // Get the item
      baseQuery <- planItemTableQuery.filter(_.id === planItemId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) => record
    }
  }

  override def getPlanItemDocumentation(planItemId: String, user: UserIdentity): Future[Documentation] = {
    val query = for {
      // Get the item
      baseQuery <- planItemTableQuery.filter(_.id === planItemId)
      definitionQuery <- caseDefinitionQuery.filter(_.caseInstanceId === baseQuery.caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield (baseQuery, definitionQuery)

    db.run(query.result.headOption).map {
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) =>
        val definitionId = record._1.definitionId
        val definitionDocument = record._2.definitions
        val element: CMMNElementDefinition = definitionDocument.findElement(element => definitionId.equals(element.getId))
        element == null match {
          case true => Documentation("")
          case _ => Documentation(element.documentation.text, element.documentation.textFormat)
        }
    }
  }

  //  override def getCasesStats(user: UserIdentity, tenant: Option[String], from: Int, numOfResults: Int, caseName: Option[String] = None, status: Option[String] = None): Future[Seq[CaseList]] = {
//    // TODO
//    // Query must be converted to:
//    //   select count(*), case_name, state, failures from case_instance group by case_name, state, failures [having state == and case_name == ]
//    val definitionFilter = caseName.getOrElse("")
//    val statusFilter = status.getOrElse("")
//
//    val tenantSet = tenant match {
//      case Some(tenant) => Seq(tenant)
//      case None => user.tenants
//    }
//    // NOTE: this uses a direct query. Fields must be escaped with quotes for the in-memory Hsql Database usage.
//    val action = {
//      status match {
//        //        case Some(s) => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = '#$s' """.as[(Long, String, String, String, Int)]
//        case Some(s) => s.toLowerCase() match {
//          case "active" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Active'""".as[(Long, String, String, String, Int)]
//          case "completed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Completed'""".as[(Long, String, String, String, Int)]
//          case "terminated" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Terminated'""".as[(Long, String, String, String, Int)]
//          case "suspended" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Suspended'""".as[(Long, String, String, String, Int)]
//          case "failed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Failed'""".as[(Long, String, String, String, Int)]
//          case "closed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Closed'""".as[(Long, String, String, String, Int)]
//          case other => throw new SearchFailure(s"Status $other is invalid")
//        }
//        case None => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" """.as[(Long, String, String, String, Int)]
//      }
//
//    }
//    db.run(action).map { value =>
//      val r = collection.mutable.Map.empty[String, CaseList]
//      value.filter(caseInstance => tenantSet.contains(caseInstance._2)).foreach { caseInstance =>
//        val count = caseInstance._1
//        val caseName = caseInstance._3
//        if (definitionFilter == "" || definitionFilter == caseName) {
//          // Only add stats for a certain filter if it is the same
//          val state = caseInstance._4
//          val failures = caseInstance._5
//
//          if (statusFilter == "" || statusFilter == state) {
//            val caseList: CaseList = r.getOrElse(caseName, CaseList(caseName = caseName))
//            val newCaseList: CaseList = {
//              var list: CaseList = state match {
//                case "Active" => caseList.copy(numActive = caseList.numActive + count)
//                case "Completed" => caseList.copy(numCompleted = caseList.numCompleted + count)
//                case "Terminated" => caseList.copy(numTerminated = caseList.numTerminated + count)
//                case "Suspended" => caseList.copy(numSuspended = caseList.numSuspended + count)
//                case "Failed" => caseList.copy(numFailed = caseList.numFailed + count)
//                case "Closed" => caseList.copy(numClosed = caseList.numClosed + count)
//                case _ => caseList
//              }
//              if (failures > 0) {
//                list = list.copy(numWithFailures = caseList.numWithFailures + count)
//              }
//              list
//            }
//            r.put(caseName, newCaseList)
//          }
//        }
//      }
//      r.values.toSeq
//    }
//  }

  override def getMyCases(user: UserIdentity, filter: CaseFilter, area: Area, sort: Sort): Future[Seq[CaseRecord]] = {
    // Note: you can only get cases if you are in the team ... This query and route makes no sense any more
    getCases(user, filter, area, sort)
  }

  override def getCases(user: UserIdentity, filter: CaseFilter, area: Area, sort: Sort): Future[Seq[CaseRecord]] = {
    val query = for {
      baseQuery <- statusFilter(filter.status)
        .filterOpt(filter.tenant)((t, value) => t.tenant === value)
        .filterOpt(filter.rootCaseId)((t, value) => t.rootCaseId === value)
        .filterOpt(filter.caseName)((t, value) => t.caseName.toLowerCase like s"%${value.toLowerCase}%")

      // Validate team membership
      _ <- membershipQuery(user, baseQuery.id, filter.identifiers)
    } yield baseQuery
    db.run(query.distinct.only(area).order(sort).result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      records
    })
  }

  def statusFilter(status: Option[String]): Query[CaseInstanceTable, CaseRecord, Seq] = {
    // Depending on the value of the "status" filter, we have 3 different filters.
    // Reason is that admin-ui uses status=Failed for both Failed and "cases with Failures"
    //  Better approach is to simply add a failure count to the case instance and align the UI for that.

    status match {
      case None => caseInstanceQuery
      case Some(state) => state match {
        case "Failed" => caseInstanceQuery.filter(_.failures > 0)
        case other => caseInstanceQuery.filter(_.state === other)
      }
    }
  }
}