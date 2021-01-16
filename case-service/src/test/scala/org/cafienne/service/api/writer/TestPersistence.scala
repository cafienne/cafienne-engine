package org.cafienne.service.api.writer

import akka.Done
import org.cafienne.cmmn.akka.command.platform.NewUserInformation
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.projection.record._

import java.time.Instant
import scala.concurrent.Future

class TestPersistence() extends RecordsPersistence {
  var records: Seq[AnyRef] = Seq()

  override def bulkUpdate(objs: Seq[AnyRef]): Future[Done] = {
    records = records ++ objs
    Future.successful(Done)
  }

  override def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]] = Future.successful(None)
  override def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]] = Future.successful(None)
  override def getTask(taskId: String): Future[Option[TaskRecord]] = Future.successful(Some(TaskRecord(id = "1", caseInstanceId = "1", tenant = "tenant", createdOn = Instant.now, lastModified = Instant.now)))
  override def getCaseInstance(id: String): Future[Option[CaseRecord]] =  Future.successful(None)
  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]] = Future.successful(None)

  override def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation]): Future[Done] = Future.successful(Done)
}
