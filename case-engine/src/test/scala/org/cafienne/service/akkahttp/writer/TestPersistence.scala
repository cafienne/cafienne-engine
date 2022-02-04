package org.cafienne.service.akkahttp.writer

import akka.Done
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.querydb.materializer.RecordsPersistence
import org.cafienne.querydb.record._

import java.time.Instant
import scala.concurrent.Future

class TestPersistence() extends RecordsPersistence {

  var records: Seq[AnyRef] = Seq()

  override def upsert(record: AnyRef) = {
    records = records ++ Seq(record)
  }

  override def delete(record: AnyRef): Unit = {
    throw new IllegalArgumentException("Deletion probably requires an implementation in testing")
  }

  override def removeCaseRoles(caseInstanceId: String): Unit = {
  }

  override def commit(): Future[Done] = Future.successful(Done)

  override def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]] = Future.successful(None)

  override def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]] = Future.successful(None)

  override def getTask(taskId: String): Future[Option[TaskRecord]] = Future.successful(Some(TaskRecord(id = "1", caseInstanceId = "1", tenant = "tenant", createdOn = Instant.now, lastModified = Instant.now)))

  override def getCaseInstance(id: String): Future[Option[CaseRecord]] = Future.successful(None)

  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]] = Future.successful(None)

  override def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done] = Future.successful(Done)

  override def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done] = Future.successful(Done)

}
