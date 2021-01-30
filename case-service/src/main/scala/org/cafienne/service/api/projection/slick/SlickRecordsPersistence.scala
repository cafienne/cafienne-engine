package org.cafienne.service.api.projection.slick

import akka.Done
import akka.persistence.query.Offset
import org.cafienne.cmmn.akka.command.platform.NewUserInformation
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

  override def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offsetName: String, offset: Offset): Future[Done] = {
    val updateQueries = info.filter(u => u.newUserId != u.existingUserId).map(user => {
      (for {c <- TableQuery[UserRoleTable].filter(r => r.userId === user.existingUserId && r.tenant === tenant)} yield c.userId).update(user.newUserId)
    }) ++ getOffsetRecord(offsetName, offset)
    db.run(DBIO.sequence(updateQueries).transactionally).map { _ => Done }
  }

//  var nr = 0L
  def getOffsetRecord(offsetName: String, offset: Offset) = {
//    println(s"$nr: Updating $offsetName to $offset")
//    nr += 1
    Seq(offsetQuery.insertOrUpdate(OffsetRecord(offsetName, offset)))
  }

  def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offsetName: String, offset: Offset): Future[Done] = {
    val updateQueries = info.map(user => {
      // Update 'createdBy' field in case instance table
      (for {cases <- TableQuery[CaseInstanceTable].filter(r => r.id === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in case instance table
      (for {cases <- TableQuery[CaseInstanceTable].filter(r => r.id === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'createdBy' field in planitem table
      (for {cases <- TableQuery[PlanItemTable].filter(r => r.caseInstanceId === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in planitem table
      (for {cases <- TableQuery[PlanItemTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in planitemhistory table
      (for {cases <- TableQuery[PlanItemHistoryTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'createdBy' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'assignee' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.assignee === user.existingUserId)} yield cases.assignee).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'owner' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.owner === user.existingUserId)} yield cases.owner).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'memberId' field in team table
      (for {cases <- TableQuery[CaseInstanceTeamMemberTable].filter(r => r.isTenantUser && r.memberId === user.existingUserId)} yield cases.memberId).update(user.newUserId)
    }) ++ getOffsetRecord(offsetName, offset)
    db.run(DBIO.sequence(updateQueries).transactionally).map { _ => Done }
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
