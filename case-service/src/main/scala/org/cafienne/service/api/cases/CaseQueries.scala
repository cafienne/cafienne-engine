package org.cafienne.service.api.cases

import akka.actor.{ActorRefFactory, ActorSystem}
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember, MemberKey}
import org.cafienne.service.api.cases.table.{CaseRecord, CaseTables, CaseTeamMemberRecord}
import org.cafienne.service.api.projection.{CaseSearchFailure, PlanItemSearchFailure, SearchFailure}
import org.cafienne.service.api.tasks.TaskTables
import org.cafienne.service.api.tenant.{TenantTables, UserRoleRecord}

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

  def getCasesStats(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String], status: Option[String]): Future[Seq[CaseList]] = ??? // GetCaseList

  def getMyCases(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String], status: Option[String]): Future[Seq[CaseRecord]] = ???

  def getCases(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String], status: Option[String]): Future[Seq[CaseRecord]] = ???
}

class CaseQueriesImpl(implicit val system: ActorSystem, implicit val actorRefFactory: ActorRefFactory)
  extends CaseQueries
    with CaseTables
    with TaskTables
    with TenantTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val caseInstanceQuery = TableQuery[CaseInstanceTable]
  val caseFileQuery = TableQuery[CaseFileTable]

  val planItemTableQuery = TableQuery[PlanItemTable]
  val taskQuery = TableQuery[TaskTable]
  val caseInstanceRoleQuery = TableQuery[CaseInstanceRoleTable]
  val caseInstanceTeamMemberQuery = TableQuery[CaseInstanceTeamMemberTable]
  val rolesQuery = TableQuery[UserRoleTable]

  override def authorizeCaseAccessAndReturnTenant(caseInstanceId: String, user: PlatformUser): Future[String] = {
    val query = for {
      // Get the case
      baseQuery <- caseInstanceQuery.filter(_.id === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant)

    } yield baseQuery.tenant

    db.run(query.result.headOption).map {
      case None => throw CaseSearchFailure(caseInstanceId)
      case Some(tenant) => tenant
    }
  }
  private def fillCaseTeam(records: Seq[CaseTeamMemberRecord]): CaseTeam = {
    def updateMember(member: CaseTeamMember, record: CaseTeamMemberRecord): CaseTeamMember = {
      record.caseRole.isBlank match {
        // If role is blank we check primary fields such as member type and ownership
        case true => member.copy(key = member.key, isOwner = Some(record.isOwner))
        // Otherwise we simply copy the roles
        case false => member.copy(caseRoles = member.caseRoles ++ Seq(record.caseRole))
      }
    }

    def key(record: CaseTeamMemberRecord): MemberKey = record.isTenantUser match {
      case true => MemberKey(record.memberId, "user")
      case false => MemberKey(record.memberId, "role")
    }

    val members = scala.collection.mutable.HashMap[MemberKey, CaseTeamMember]()
    records.map(record => {
      val memberKey = key(record)
      val memberToUpdate = members.getOrElse(memberKey, CaseTeamMember(memberKey))
      members.put(memberKey, updateMember(memberToUpdate, record))
    })

    CaseTeam(members.map(m => m._2).toSeq)
  }

  override def getFullCaseInstance(caseInstanceId: String, user: PlatformUser): Future[FullCase] = {
    val result = for {
      caseInstance <- getCaseInstance(caseInstanceId, user)
      caseTeam <- db.run(caseInstanceTeamMemberQuery.filter(_.caseInstanceId === caseInstanceId).filter(_.active === true).result).map{fillCaseTeam}
      caseFile <- db.run(caseFileQuery.filter(_.caseInstanceId === caseInstanceId).result.headOption).map(f => CaseFile(f.getOrElse(null)))
      casePlan <- db.run(planItemTableQuery.filter(_.caseInstanceId === caseInstanceId).result).map{CasePlan}
    } yield (caseInstance, caseTeam, caseFile, casePlan)

    result.map(x => x._1.fold(throw CaseSearchFailure(caseInstanceId))(caseRecord => FullCase(caseRecord, file = x._3, team = x._2, planitems = x._4)))
  }

  /**
    * Query that validates that the user belongs to the team of the specified case, either by explicit
    * membership of the user id, or by one of the tenant roles of the user that are bound to the team of the case
    * @param user
    * @param caseInstanceId
    * @param tenant
    * @return
    */
  private def membershipQuery(user: PlatformUser, caseInstanceId: Rep[String], tenant: Rep[String]): Query[(UserRoleTable, CaseInstanceTeamMemberTable), (UserRoleRecord, CaseTeamMemberRecord), Seq] = {
    val query = for {
      // Validate tenant membership
      tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === tenant)
      // Validate case team membership: either user is explicit member or has a matching tenant role
      teamMembership <- TableQuery[CaseInstanceTeamMemberTable]
        .filter(_.caseInstanceId === caseInstanceId)
        .filter(_.active === true) // Only search in active team members
        .filter(_.caseRole === "") // Only search by base membership, not in certain roles
        .filter(member => { // Search by user id or by one of the user's tenant roles
          (member.isTenantUser === true && member.memberId === user.userId) ||
            (member.isTenantUser === false && member.memberId === tenantMembership.role_name)
        })
    } yield (tenantMembership, teamMembership)

    query
  }

  override def getCaseInstance(caseInstanceId: String, user: PlatformUser): Future[Option[CaseRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- caseInstanceQuery.filter(_.id === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.result.headOption)
  }

  override def getCaseFile(caseInstanceId: String, user: PlatformUser): Future[CaseFile] = {
    val query = for {
      // Get the case file
      baseQuery <- caseFileQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant)
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
      // Access control query
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      fillCaseTeam(records)
    })
  }

  override def getPlanItems(caseInstanceId: String, user: PlatformUser): Future[CasePlan] = {
    val query = for {
      // Get the items in the case plan
      baseQuery <- planItemTableQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      CasePlan(records)
    })
  }

  override def getPlanItem(planItemId: String, user: PlatformUser): Future[PlanItem] = {
    val query = for {
      // Get the item
      baseQuery <- planItemTableQuery.filter(_.id === planItemId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.result.headOption).map{
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) => PlanItem(record)
    }
  }

  override def getPlanItemHistory(planItemId: String, user: PlatformUser): Future[PlanItemHistory] = {
    val query = for {
      // Get the item's history
      baseQuery <- TableQuery[PlanItemHistoryTable].filter(_.planItemId === planItemId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw PlanItemSearchFailure(planItemId)
      PlanItemHistory(records)
    })
  }

  override def getCasesStats(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String] = None, status: Option[String] = None): Future[Seq[CaseList]] = {
    // TODO
    // Query must be converted to:
    //   select count(*), definition, state, failures from case_instance group by definition, state, failures [having state == and definition == ]
    val definitionFilter = definition.getOrElse("")
    val statusFilter = status.getOrElse("")

    val tenantSet = tenants(tenant, user)
    // NOTE: this uses a direct query. Fields must be escaped with quotes for the in-memory Hsql Database usage.
    val action = {
      status match {
//        case Some(s) => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = '#$s' """.as[(Long, String, String, String, Int)]
        case Some(s) => s.toLowerCase() match {
          case "active" => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = 'Active'""".as[(Long, String, String, String, Int)]
          case "completed" => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = 'Completed'""".as[(Long, String, String, String, Int)]
          case "terminated" => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = 'Terminated'""".as[(Long, String, String, String, Int)]
          case "suspended" => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = 'Suspended'""".as[(Long, String, String, String, Int)]
          case "failed" => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = 'Failed'""".as[(Long, String, String, String, Int)]
          case "closed" => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" having "state" = 'Closed'""".as[(Long, String, String, String, Int)]
          case other => throw new SearchFailure(s"Status $other is invalid")
        }
        case None => sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" """.as[(Long, String, String, String, Int)]
      }

    }
    db.run(action).map { value =>
      val r = collection.mutable.Map.empty[String, CaseList]
      value.filter(caseInstance => tenantSet.contains(caseInstance._2)).foreach { caseInstance =>
        val count = caseInstance._1
        val definition = caseInstance._3
        if (definitionFilter == "" || definitionFilter == definition) {
          // Only add stats for a certain filter if it is the same
          val state = caseInstance._4
          val failures = caseInstance._5

          if (statusFilter == "" || statusFilter == state) {
            val caseList: CaseList = r.getOrElse(definition, CaseList(definition = definition))
            val newCaseList: CaseList = updateCaseList(state, count, failures, caseList)
            r.put(definition, newCaseList)
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

  override def getMyCases(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String], status: Option[String]): Future[Seq[CaseRecord]] = {
    // Note: you can only get cases if you are in the team ... This query and route makes no sense any more
    getCases(tenant, from, numOfResults, user, definition, status)
  }

  override def getCases(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String], status: Option[String]): Future[Seq[CaseRecord]] = {
    // Depending on the value of the "status" filter, we have 3 different filters.
    // Reason is that admin-ui uses status=Failed for both Failed and "cases with Failures"
    //  Better approach is to simply add a failure count to the case instance and align the UI for that.

    val stateFilterQuery = status match {
      case None => caseInstanceQuery
      case Some(state) => state match {
        case "Failed" => caseInstanceQuery.filter(_.failures > 0)
        case other => caseInstanceQuery.filter(_.state === other)
      }
    }

    val query = for {
      baseQuery <- stateFilterQuery
        .optionFilter(tenant)((t, value) => t.tenant === value)
        .optionFilter(definition)((t, value) => t.definition.toLowerCase like s"%${value.toLowerCase}%")
        .sortBy(_.lastModified.desc)
        .drop(from).take(numOfResults)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.id, baseQuery.tenant)
    } yield baseQuery

    db.run(query.distinct.result)
  }
}