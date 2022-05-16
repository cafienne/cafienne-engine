package org.cafienne.querydb.materializer.cases

import akka.Done
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.TestQueryDBTransaction
import org.cafienne.querydb.materializer.cases.team.CaseTeamMemberKey
import org.cafienne.querydb.record._

import java.time.Instant
import scala.concurrent.Future

class TestCaseStorageTransaction(caseInstanceId: String) extends TestQueryDBTransaction(caseInstanceId) with CaseStorageTransaction {
  override def upsert(record: CaseRecord): Unit = {
    println("Adding case record")
    addRecord(record)
  }

  override def upsert(record: CaseDefinitionRecord): Unit = addRecord(record)

  override def upsert(record: TaskRecord): Unit = addRecord(record)

  override def upsert(record: PlanItemRecord): Unit = addRecord(record)

  override def upsert(record: PlanItemHistoryRecord): Unit = addRecord(record)

  override def upsert(record: CaseFileRecord): Unit = addRecord(record)

  override def upsert(record: CaseBusinessIdentifierRecord): Unit = addRecord(record)

  override def upsert(record: CaseRoleRecord): Unit = addRecord(record)

  override def upsert(record: CaseTeamUserRecord): Unit = addRecord(record)

  override def upsert(record: CaseTeamTenantRoleRecord): Unit = addRecord(record)

  override def upsert(record: CaseTeamGroupRecord): Unit = addRecord(record)

  override def delete(record: CaseTeamUserRecord): Unit = ???

  override def delete(record: CaseTeamTenantRoleRecord): Unit = ???

  override def delete(record: CaseTeamGroupRecord): Unit = ???

  override def deleteTaskRecord(taskId: String): Unit = ???

  override def deleteCaseTeamMember(key: CaseTeamMemberKey): Unit = ???

  override def deletePlanItemRecordAndHistory(planItemId: String): Unit = ???

  override def removeCaseRoles(caseInstanceId: String): Unit = {}

  override def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]] = Future.successful(None)

  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]] = Future.successful(None)

  override def getCaseInstance(id: String): Future[Option[CaseRecord]] = Future.successful(None)

  override def getTask(taskId: String): Future[Option[TaskRecord]] = Future.successful(Some(TaskRecord(id = "1", caseInstanceId = "1", tenant = "tenant", createdOn = Instant.now, lastModified = Instant.now)))

  override def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done] = Future.successful(Done)

}
