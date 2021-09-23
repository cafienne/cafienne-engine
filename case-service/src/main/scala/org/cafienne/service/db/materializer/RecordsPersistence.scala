package org.cafienne.service.db.materializer

import akka.Done
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.service.db.record._

import scala.concurrent.Future

trait RecordsPersistence {

  //  def upsert[M: AnyRef, T <: CafienneTable[M]](record: M)
  // Not sure how to get something like above working here, would actually be great if we can achieve that (use ClassTag or so?)
  def upsert(record: AnyRef): Unit

  def delete(record: AnyRef): Unit

  def deleteTaskRecord(taskId: String): Unit = ???

  def deletePlanItemRecordAndHistory(planItemId: String): Unit = ???

  def removeCaseRoles(caseInstanceId: String): Unit

  def commit(): Future[Done]

  def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]]

  def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]]

  def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]]

  def getCaseInstance(caseInstanceId: String): Future[Option[CaseRecord]]

  def getTask(taskId: String): Future[Option[TaskRecord]]

  def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done]

  def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done]
}
