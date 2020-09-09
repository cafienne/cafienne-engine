package org.cafienne.service.api.projection.query

import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember, MemberKey}
import org.cafienne.service.api.cases._
import org.cafienne.service.api.projection.record.{CaseRecord, CaseRoleRecord, CaseTeamMemberRecord}
import org.cafienne.service.api.projection.{CaseSearchFailure, PlanItemSearchFailure, SearchFailure}

import scala.concurrent.Future

trait CaseQueries {
  def authorizeCaseAccessAndReturnTenant(caseInstanceId: String, user: PlatformUser): Future[String] = ???

  def getFullCaseInstance(caseInstanceId: String, user: PlatformUser): Future[FullCase] = ???

  def getCaseInstance(caseInstanceId: String, user: PlatformUser): Future[Option[CaseRecord]] = ???

  def getCaseFile(caseInstanceId: String, user: PlatformUser): Future[CaseFile] = ???

  def getCaseTeam(caseInstanceId: String, user: PlatformUser): Future[CaseTeam] = ???

  def getPlanItems(caseInstanceId: String, user: PlatformUser): Future[CasePlan] = ???

  def getPlanItem(planItemId: String, user: PlatformUser): Future[PlanItem] = ???

  def getPlanItemHistory(planItemId: String, user: PlatformUser): Future[PlanItemHistory] = ???

  def getCasesStats(user: PlatformUser, tenant: Option[String], from: Int, numOfResults: Int, caseName: Option[String], status: Option[String]): Future[Seq[CaseList]] = ??? // GetCaseList

  def getMyCases(user: PlatformUser, filter: CaseFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[CaseRecord]] = ???

  def getCases(user: PlatformUser, filter: CaseFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[CaseRecord]] = ???
}

class CaseQueriesImpl
  extends CaseQueries
    with BaseQueryImpl {

  import dbConfig.profile.api._

  override def authorizeCaseAccessAndReturnTenant(caseInstanceId: String, user: PlatformUser): Future[String] = {
    val query = for {
      // Get the case
      baseQuery <- caseInstanceQuery.filter(_.id === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant, None)

    } yield baseQuery.tenant

    db.run(query.result.headOption).map {
      case None => throw CaseSearchFailure(caseInstanceId)
      case Some(tenant) => tenant
    }
  }

  override def getFullCaseInstance(caseInstanceId: String, user: PlatformUser): Future[FullCase] = {
    val result = for {
      caseInstance <- getCaseInstance(caseInstanceId, user)
      caseTeam <- db.run(caseInstanceTeamMemberQuery.filter(_.caseInstanceId === caseInstanceId).filter(_.active === true).result).map {
        fillCaseTeam
      }
      caseFile <- db.run(caseFileQuery.filter(_.caseInstanceId === caseInstanceId).result.headOption).map(f => CaseFile(f.getOrElse(null)))
      casePlan <- db.run(planItemTableQuery.filter(_.caseInstanceId === caseInstanceId).result).map {
        CasePlan
      }
    } yield (caseInstance, caseTeam, caseFile, casePlan)

    result.map(x => x._1.fold(throw CaseSearchFailure(caseInstanceId))(caseRecord => FullCase(caseRecord, file = x._3, team = x._2, planitems = x._4)))
  }

  override def getCaseInstance(caseInstanceId: String, user: PlatformUser): Future[Option[CaseRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- caseInstanceQuery.filter(_.id === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.result.headOption)
  }

  override def blankIdentifierFilterQuery(caseInstanceId: Rep[String]) = {
    TableQuery[CaseInstanceTable].filter(_.id === caseInstanceId)
  }

  override def getCaseFile(caseInstanceId: String, user: PlatformUser): Future[CaseFile] = {
    val query = for {
      // Get the case file
      baseQuery <- caseFileQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => CaseFile(result)
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getCaseTeam(caseInstanceId: String, user: PlatformUser): Future[CaseTeam] = {
    val query = for {
      // Get the case team
      baseQuery <- caseInstanceTeamMemberQuery
        .filter(_.caseInstanceId === caseInstanceId)
        .filter(_.active === true)
        .joinRight(TableQuery[CaseInstanceRoleTable]) // Select all defined case roles; joining right will also return unfilled case roles
        .on((m, r) => m.caseInstanceId === r.caseInstanceId && m.caseRole === r.roleName)
        .filter(_._2.caseInstanceId === caseInstanceId) // but only for this case obviously
      // Access control query
      _ <- membershipQuery(user, caseInstanceId, baseQuery._2.tenant, None)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      val team = fillCaseTeam(records.map(r => r._1).filter(m => m.nonEmpty).map(m => m.get))
      val unassignedRoles = records.filter(r => r._1.isEmpty).map(r => r._2.roleName)
      val caseRoles = records.map(r => r._2.roleName).filterNot(_.isBlank).toSet.toSeq
      team.copy(caseRoles = caseRoles, unassignedRoles = unassignedRoles)
    })
  }

  private def fillCaseTeam(records: Seq[CaseTeamMemberRecord]): CaseTeam = {
    val members = records.filter(record => record.caseRole.isBlank)
    val roles = records.filterNot(record => record.caseRole.isBlank)

    CaseTeam(members.map(member => {
      val key = MemberKey(member.memberId, member.isTenantUser match { case true => {
        "user"
      }
      case false => {
        "role"
      }
      })
      val memberRoles = roles.filter(role => role.memberId == member.memberId && role.isTenantUser == member.isTenantUser).map(role => role.caseRole)
      new CaseTeamMember(key, memberRoles, Some(member.isOwner))
    }))
  }

  override def getPlanItems(caseInstanceId: String, user: PlatformUser): Future[CasePlan] = {
    val query = for {
      // Get the items in the case plan
      baseQuery <- planItemTableQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      CasePlan(records)
    })
  }

  override def getPlanItem(planItemId: String, user: PlatformUser): Future[PlanItem] = {
    val query = for {
      // Get the item
      baseQuery <- planItemTableQuery.filter(_.id === planItemId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) => PlanItem(record)
    }
  }

  override def getPlanItemHistory(planItemId: String, user: PlatformUser): Future[PlanItemHistory] = {
    val query = for {
      // Get the item's history
      baseQuery <- TableQuery[PlanItemHistoryTable].filter(_.planItemId === planItemId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw PlanItemSearchFailure(planItemId)
      PlanItemHistory(records)
    })
  }

  override def getCasesStats(user: PlatformUser, tenant: Option[String], from: Int, numOfResults: Int, caseName: Option[String] = None, status: Option[String] = None): Future[Seq[CaseList]] = {
    // TODO
    // Query must be converted to:
    //   select count(*), case_name, state, failures from case_instance group by case_name, state, failures [having state == and case_name == ]
    val definitionFilter = caseName.getOrElse("")
    val statusFilter = status.getOrElse("")

    val tenantSet = tenants(tenant, user)
    // NOTE: this uses a direct query. Fields must be escaped with quotes for the in-memory Hsql Database usage.
    val action = {
      status match {
        //        case Some(s) => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = '#$s' """.as[(Long, String, String, String, Int)]
        case Some(s) => s.toLowerCase() match {
          case "active" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Active'""".as[(Long, String, String, String, Int)]
          case "completed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Completed'""".as[(Long, String, String, String, Int)]
          case "terminated" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Terminated'""".as[(Long, String, String, String, Int)]
          case "suspended" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Suspended'""".as[(Long, String, String, String, Int)]
          case "failed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Failed'""".as[(Long, String, String, String, Int)]
          case "closed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Closed'""".as[(Long, String, String, String, Int)]
          case other => throw new SearchFailure(s"Status $other is invalid")
        }
        case None => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" """.as[(Long, String, String, String, Int)]
      }

    }
    db.run(action).map { value =>
      val r = collection.mutable.Map.empty[String, CaseList]
      value.filter(caseInstance => tenantSet.contains(caseInstance._2)).foreach { caseInstance =>
        val count = caseInstance._1
        val caseName = caseInstance._3
        if (definitionFilter == "" || definitionFilter == caseName) {
          // Only add stats for a certain filter if it is the same
          val state = caseInstance._4
          val failures = caseInstance._5

          if (statusFilter == "" || statusFilter == state) {
            val caseList: CaseList = r.getOrElse(caseName, CaseList(caseName = caseName))
            val newCaseList: CaseList = updateCaseList(state, count, failures, caseList)
            r.put(caseName, newCaseList)
          }
        }
      }
      r.values.toSeq
    }
  }

  // It will be nice if this code can be similar to the optionFilter class
  private def tenants(optionalTenant: Option[String], user: PlatformUser): Seq[String] = {
    optionalTenant match {
      case Some(tenant) => Seq(tenant)
      case None => user.tenants
    }
  }

  private def updateCaseList(state: String, count: Long, failures: Int, caseList: CaseList) = {
    var newCaseList: CaseList = state match {
      case "Active" => caseList.copy(numActive = caseList.numActive + count)
      case "Completed" => caseList.copy(numCompleted = caseList.numCompleted + count)
      case "Terminated" => caseList.copy(numTerminated = caseList.numTerminated + count)
      case "Suspended" => caseList.copy(numSuspended = caseList.numSuspended + count)
      case "Failed" => caseList.copy(numFailed = caseList.numFailed + count)
      case "Closed" => caseList.copy(numClosed = caseList.numClosed + count)
      case _ => caseList
    }
    if (failures > 0) {
      newCaseList = newCaseList.copy(numWithFailures = caseList.numWithFailures + count)
    }
    newCaseList
  }

  override def getMyCases(user: PlatformUser, filter: CaseFilter, area: Area, sort: Sort): Future[Seq[CaseRecord]] = {
    // Note: you can only get cases if you are in the team ... This query and route makes no sense any more
    getCases(user, filter, area, sort)
  }

  override def getCases(user: PlatformUser, filter: CaseFilter, area: Area, sort: Sort): Future[Seq[CaseRecord]] = {
    val query = for {
      baseQuery <- statusFilter(filter.status)
          .filterOpt(filter.tenant)((t, value) => t.tenant === value)
          .filterOpt(filter.caseName)((t, value) => t.caseName.toLowerCase like s"%${value.toLowerCase}%")

      // Validate team membership
      _ <- membershipQuery(user, baseQuery.id, baseQuery.tenant, filter.identifiers)
    } yield baseQuery
    db.run(query.distinct.only(area).order(sort).result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      records
    })
  }

  def statusFilter(status: Option[String]) = {
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