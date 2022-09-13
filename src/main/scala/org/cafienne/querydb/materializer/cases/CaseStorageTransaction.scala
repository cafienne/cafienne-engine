package org.cafienne.querydb.materializer.cases

import akka.Done
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.materializer.cases.team.CaseTeamMemberKey
import org.cafienne.querydb.record._

import scala.concurrent.Future

trait CaseStorageTransaction extends QueryDBTransaction {
  def upsert(record: CaseRecord): Unit

  def upsert(record: CaseDefinitionRecord): Unit

  def upsert(record: TaskRecord): Unit

  def upsert(record: PlanItemRecord): Unit

  def upsert(record: CaseFileRecord): Unit

  def upsert(record: CaseBusinessIdentifierRecord): Unit

  def upsert(record: CaseRoleRecord): Unit

  def upsert(record: CaseTeamUserRecord): Unit

  def upsert(record: CaseTeamTenantRoleRecord): Unit

  def upsert(record: CaseTeamGroupRecord): Unit

  def delete(record: CaseTeamUserRecord): Unit

  def delete(record: CaseTeamTenantRoleRecord): Unit

  def delete(record: CaseTeamGroupRecord): Unit

  def deleteTaskRecord(taskId: String): Unit

  def deleteCaseTeamMember(key: CaseTeamMemberKey): Unit

  def deletePlanItemRecord(planItemId: String): Unit

  def removeCaseRoles(caseInstanceId: String): Unit

  def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]]

  def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]]

  def getCaseInstance(caseInstanceId: String): Future[Option[CaseRecord]]

  def getTask(taskId: String): Future[Option[TaskRecord]]

  def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done]
}
