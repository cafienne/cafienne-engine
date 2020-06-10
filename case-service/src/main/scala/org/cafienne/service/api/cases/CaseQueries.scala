package org.cafienne.service.api.cases

import akka.actor.{ActorRefFactory, ActorSystem}
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.service.api.cases.table.{CaseRecord, CaseTables}
import org.cafienne.service.api.projection.{CaseSearchFailure, PlanItemSearchFailure}
import org.cafienne.service.api.tasks.TaskTables
import org.cafienne.service.api.tenant.TenantTables

import scala.concurrent.Future

trait CaseQueries {
  def authorizeCaseAccess(caseInstanceId: String, user: PlatformUser): Future[String] = ???

  def getFullCaseInstance(caseInstanceId: String, user: PlatformUser): Future[FullCase] = ???

  def getCaseInstance(caseInstanceId: String, user: PlatformUser): Future[Option[CaseRecord]] = ???

  def getCaseFile(caseInstanceId: String, user: PlatformUser): Future[CaseFile] = ???

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


  // It will be nice if this code can be similar to the optionFilter class
  private def tenants(optionalTenant: Option[String], user: PlatformUser): Seq[String] = {
    optionalTenant match {
      case Some(tenant) => Seq(tenant)
      case None => user.tenants
    }
  }

  override def authorizeCaseAccess(caseInstanceId: String, user: PlatformUser): Future[String] = {

    // LEFT JOIN on task and user; left join so that we know if the task exists and/or if the user exists and is enabled/disabled
    val query = for {
      (c, s) <- TableQuery[CaseInstanceTable].filter(_.id === caseInstanceId) joinLeft TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.role_name === "") on (_.tenant === _.tenant)
    } yield (c.tenant, s.map(_.userId), s.map(_.enabled))

    db.run(query.result.headOption).map {
      case Some((tenant: String, userId: Option[String], enabled: Option[Boolean])) => {
        (userId, enabled) match {
          case (None, None) => {
//            System.out.println("User does not even exist or not in this tenant")
            throw new SecurityException("User does not exist")
          }
          case (Some(id), Some(false)) => {
//            System.out.println("User "+id+" is disabled, cannot give the case")
            throw new SecurityException("User is not allowed to access case")
          }
          case (Some(id), Some(true)) => {
//            System.out.println("Everythign just fine; go ahead with your case, user "+id)
            tenant
          }
          case (_, _) => throw new SecurityException("I should never reach this block of code")
        }
      }
      case None => {
        throw CaseSearchFailure(caseInstanceId)
      }
    }
  }

  override def getFullCaseInstance(caseInstanceId: String, user: PlatformUser): Future[FullCase] = {
    val result = for {
      caseInstance <- getCaseInstance(caseInstanceId, user)
      caseTeam <- db.run(caseInstanceTeamMemberQuery.filter(_.caseInstanceId === caseInstanceId).filter(_.active === true).result).map{CaseTeam}
      caseFile <- db.run(caseFileQuery.filter(_.caseInstanceId === caseInstanceId).result.headOption).map(f => CaseFile(f.getOrElse(null)))
      casePlan <- db.run(planItemTableQuery.filter(_.caseInstanceId === caseInstanceId).result).map{CasePlan}
    } yield (caseInstance, caseTeam, caseFile, casePlan)

    result.map(x => x._1.fold(throw CaseSearchFailure(caseInstanceId))(caseRecord => FullCase(caseRecord, file = x._3, team = x._2, planitems = x._4)))
  }

  override def getCaseInstance(id: String, user: PlatformUser): Future[Option[CaseRecord]] = {
    val query = caseInstanceQuery
      .filter(_.id === id)
      .filter(_.tenant.inSet(user.tenants))
    db.run(query.result.headOption)
  }

  override def getCaseFile(caseInstanceId: String, user: PlatformUser): Future[CaseFile] = {
    val query = caseFileQuery
      .filter(_.caseInstanceId === caseInstanceId)
      .filter(_.tenant.inSet(user.tenants))
    db.run(query.result.headOption).map {
      case Some(result) => CaseFile(result)
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getPlanItems(caseInstanceId: String, user: PlatformUser): Future[CasePlan] = {
    val query = planItemTableQuery
      .filter(_.caseInstanceId === caseInstanceId)
      .filter(_.tenant.inSet(user.tenants))

    db.run(query.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      CasePlan(records)
    })
  }

  override def getPlanItem(planItemId: String, user: PlatformUser): Future[PlanItem] = {
    val query = planItemTableQuery
      .filter(_.id === planItemId)
      .filter(_.tenant.inSet(user.tenants))

    db.run(query.result.headOption).map{
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) => PlanItem(record)
    }
  }

  override def getPlanItemHistory(planItemId: String, user: PlatformUser): Future[PlanItemHistory] = {
    val planItemHistoryTableQuery = TableQuery[PlanItemHistoryTable]
    val query = planItemHistoryTableQuery
      .filter(_.tenant.inSet(user.tenants))
      .filter(_.planItemId === planItemId)

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw PlanItemSearchFailure(planItemId)
      PlanItemHistory(records)
    })
  }

  override def getCasesStats(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String] = None, status: Option[String] = None): Future[Seq[CaseList]] = {
    // TODO
    // Query must be converted to:
    //   select count(*), definition, state, failures from case_instance group by definition, state, failures [where state == and definition == ]
    val definitionFilter = definition.getOrElse("")
    val statusFilter = status.getOrElse("")

    val tenantSet = tenants(tenant, user)
    // NOTE: this uses a direct query. Fields must be escaped with quotes for the in-memory Hsql Database usage.
    val action = sql"""select count("definition") as count, "tenant", "definition", "state", "failures" from  "case_instance" group by "tenant", "definition", "state", "failures" """.as[(Long, String, String, String, Int)]
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

  def updateCaseList(state: String, count: Long, failures: Int, caseList: CaseList) = {
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
    val tenantSet = tenants(tenant, user)
    val query = caseInstanceQuery
      .filter(_.tenant.inSet(tenantSet))
      .optionFilter(definition)((t, value) => t.definition.toLowerCase like s"%${value.toLowerCase}%")
      .optionFilter(status)((t, value) => t.state === value)
      .sortBy(_.lastModified.desc)
      .drop(from).take(numOfResults)
    db.run(query.result)
  }

  override def getCases(tenant: Option[String], from: Int, numOfResults: Int, user: PlatformUser, definition: Option[String], status: Option[String]): Future[Seq[CaseRecord]] = {
    // Depending on the value of the "status" filter, we have 3 different queries.
    // Reason is that admin-ui uses status=Failed for both Failed and "cases with Failures"
    //  Better approach is to simply add a failure count to the case instance and align the UI for that.

    val tenantSet = tenants(tenant, user)
    val query =
      status match {
        case Some(state) => {
          state match {
            case "Failed" => {
              //              println("Filtering on failures")
              caseInstanceQuery
                .filter(_.tenant.inSet(tenantSet))
                .optionFilter(definition)((t, value) => t.definition.toLowerCase like s"%${value.toLowerCase}%")
                .optionFilter(status)((t, _) => t.failures > 0)
                .sortBy(_.lastModified.desc)
                .drop(from).take(numOfResults)
            }
            case s => {
              //              println("Filtering on another status: " + s)
              caseInstanceQuery
                .filter(_.tenant.inSet(tenantSet))
                .optionFilter(definition)((t, value) => t.definition.toLowerCase like s"%${value.toLowerCase}%")
                .optionFilter(status)((t, value) => t.state === value)
                .sortBy(_.lastModified.desc)
                .drop(from).take(numOfResults)
            }
          }
        }
        case None => {
          caseInstanceQuery
            .filter(_.tenant.inSet(tenantSet))
            .optionFilter(definition)((t, value) => t.definition.toLowerCase like s"%${value.toLowerCase}%")
            .sortBy(_.lastModified.desc)
            .drop(from).take(numOfResults)
        }
      }

    db.run(query.result)
  }

}