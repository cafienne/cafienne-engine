package org.cafienne.service.api.projection.slick

import akka.Done
import org.cafienne.infrastructure.jdbc.{OffsetStore, OffsetStoreTables}
import org.cafienne.service.api.cases.{CaseFile, CaseInstance, CaseInstanceDefinition, CaseInstanceRole, CaseInstanceTeamMember, CaseTables, PlanItem, PlanItemHistory}
import org.cafienne.service.api.tenant.{Tenant, TenantOwner, User, UserRole, TenantTables}
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tasks.{Task, TaskTables}

import scala.concurrent.Future


class SlickRecordsPersistence
  extends RecordsPersistence
    with CaseTables
    with TaskTables
    with TenantTables
    with OffsetStoreTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val caseInstanceQuery = TableQuery[CaseInstanceTable]
  val caseInstanceDefinitionQuery = TableQuery[CaseInstanceDefinitionTable]
  val caseFileQuery = TableQuery[CaseFileTable]

  val planItemTableQuery = TableQuery[PlanItemTable]
  val planItemHistoryTableQuery = TableQuery[PlanItemHistoryTable]
  val taskQuery = TableQuery[TaskTable]
  val caseInstanceRoleQuery = TableQuery[CaseInstanceRoleTable]
  val caseInstanceTeamMemberQuery = TableQuery[CaseInstanceTeamMemberTable]
  val tenantsQuery = TableQuery[TenantTable]
  val tenantOwnersQuery = TableQuery[TenantOwnersTable]
  val rolesQuery = TableQuery[UserRoleTable]
  val offsetQuery = TableQuery[OffsetStoreTable]

  override def bulkUpdate(records: Seq[AnyRef]) = {
    val actions = records.collect {
      case value: Task => taskQuery.insertOrUpdate(value)
      case value: PlanItem => planItemTableQuery.insertOrUpdate(value)
      case value: PlanItemHistory => planItemHistoryTableQuery.insertOrUpdate(value) // Enable restarting with noOffset
      case value: CaseInstance => caseInstanceQuery.insertOrUpdate(value)
      case value: CaseInstanceDefinition => caseInstanceDefinitionQuery.insertOrUpdate(value)
      case value: CaseFile => caseFileQuery.insertOrUpdate(value)
      case value: CaseInstanceRole => caseInstanceRoleQuery.insertOrUpdate(value)
      case value: CaseInstanceTeamMember => caseInstanceTeamMemberQuery.insertOrUpdate(value)
      case value: OffsetStore => offsetQuery.insertOrUpdate(value)
      case value: UserRole => rolesQuery.insertOrUpdate(value)
      case value: Tenant => tenantsQuery.insertOrUpdate(value)
      case value: TenantOwner => tenantOwnersQuery.insertOrUpdate(value)
      case other => throw new IllegalArgumentException("Bulk update not supported for objects of type " + other.getClass.getName)
    }
    db.run(DBIO.sequence(actions).transactionally).map { _ => Done }
  }

  override def getPlanItem(planItemId: String): Future[Option[PlanItem]] = {
    db.run(TableQuery[PlanItemTable].filter(_.id === planItemId).result.headOption)
  }

  override def getCaseInstance(id: String): Future[Option[CaseInstance]] = {
    db.run(TableQuery[CaseInstanceTable].filter(_.id === id).result.headOption)
  }

  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFile]] = {
    db.run(TableQuery[CaseFileTable].filter(_.caseInstanceId === caseInstanceId).result.headOption)
  }

  override def getTask(taskId: String): Future[Task] = {
    db.run(TableQuery[TaskTable].filter(_.id === taskId).result.head)
  }
}
