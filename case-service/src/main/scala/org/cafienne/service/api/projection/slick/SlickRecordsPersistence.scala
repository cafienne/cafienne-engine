package org.cafienne.service.api.projection.slick

import akka.Done
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.infrastructure.jdbc.OffsetStoreTables
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.projection.record._
import org.cafienne.service.api.projection.table.{CaseTables, TaskTables, TenantTables}

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
  val identifierQuery = TableQuery[CaseBusinessIdentifierTable]

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
      case value: TaskRecord => taskQuery.insertOrUpdate(value)
      case value: PlanItemRecord => planItemTableQuery.insertOrUpdate(value)
      case value: PlanItemHistoryRecord => planItemHistoryTableQuery.insertOrUpdate(value) // Enable restarting with noOffset
      case value: CaseRecord => caseInstanceQuery.insertOrUpdate(value)
      case value: CaseDefinitionRecord => caseInstanceDefinitionQuery.insertOrUpdate(value)
      case value: CaseFileRecord => caseFileQuery.insertOrUpdate(value)
      case value: CaseBusinessIdentifierRecord => identifierQuery.insertOrUpdate(value)
      case value: CaseRoleRecord => caseInstanceRoleQuery.insertOrUpdate(value)
      case value: CaseTeamMemberRecord => caseInstanceTeamMemberQuery.insertOrUpdate(value)
      case value: OffsetRecord => offsetQuery.insertOrUpdate(value)
      case value: UserRoleRecord => rolesQuery.insertOrUpdate(value)
      case value: TenantRecord => tenantsQuery.insertOrUpdate(value)
      case value: TenantOwnerRecord => tenantOwnersQuery.insertOrUpdate(value)
      case other => throw new IllegalArgumentException("Bulk update not supported for objects of type " + other.getClass.getName)
    }
    db.run(DBIO.sequence(actions).transactionally).map { _ => Done }
  }

  override def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]] = {
    db.run(TableQuery[PlanItemTable].filter(_.id === planItemId).result.headOption)
  }

  override def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]] = {
    db.run(TableQuery[UserRoleTable].filter(record => record.userId === key.userId && record.tenant === key.tenant && record.role_name === key.role_name).result.headOption)
  }

  override def getCaseInstance(id: String): Future[Option[CaseRecord]] = {
    db.run(TableQuery[CaseInstanceTable].filter(_.id === id).result.headOption)
  }

  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]] = {
    db.run(TableQuery[CaseFileTable].filter(_.caseInstanceId === caseInstanceId).result.headOption)
  }

  override def getTask(taskId: String): Future[Option[TaskRecord]] = {
    db.run(TableQuery[TaskTable].filter(_.id === taskId).result.headOption)
  }
}
