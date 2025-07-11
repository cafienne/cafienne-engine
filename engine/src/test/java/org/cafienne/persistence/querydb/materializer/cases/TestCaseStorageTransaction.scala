package org.cafienne.persistence.querydb.materializer.cases

import org.cafienne.engine.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.persistence.querydb.materializer.TestQueryDBTransaction
import org.cafienne.persistence.querydb.materializer.cases.team.CaseTeamMemberKey
import org.cafienne.persistence.querydb.record._

import java.time.Instant

class TestCaseStorageTransaction(caseInstanceId: String) extends TestQueryDBTransaction(caseInstanceId) with CaseStorageTransaction {
  override def upsert(record: CaseRecord): Unit = {
    println("Adding case record")
    addRecord(record)
  }

  override def upsert(record: CaseDefinitionRecord): Unit = addRecord(record)

  override def upsert(record: TaskRecord): Unit = addRecord(record)

  override def upsert(record: PlanItemRecord): Unit = addRecord(record)

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

  override def deletePlanItemRecord(planItemId: String): Unit = ???

  override def removeCaseRoles(caseInstanceId: String): Unit = {}

  override def getPlanItem(planItemId: String): Option[PlanItemRecord] = None

  override def getCaseFile(caseInstanceId: String): Option[CaseFileRecord] = None

  override def getCaseInstance(id: String): Option[CaseRecord] = None

  override def getTask(taskId: String): Option[TaskRecord] = Some(TaskRecord(id = "1", caseInstanceId = "1", tenant = "tenant", createdOn = Instant.now, lastModified = Instant.now))

  override def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Unit = {}

}
