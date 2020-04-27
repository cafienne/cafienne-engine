package org.cafienne.service.api.writer

import java.time.Instant

import akka.Done
import org.cafienne.service.api.cases.{CaseFile, CaseInstance, PlanItem}
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tasks.Task

import scala.concurrent.Future

class TestPersistence() extends RecordsPersistence {
  var records: Seq[AnyRef] = Seq()

  override def bulkUpdate(objs: Seq[AnyRef]): Future[Done] = {
    records = records ++ objs
    Future.successful(Done)
  }

  override def getPlanItem(planItemId: String): Future[Option[PlanItem]] = Future.successful(None)
  override def getTask(taskId: String): Future[Option[Task]] = Future.successful(Some(Task(id = "1", caseInstanceId = "1", tenant = "tenant", createdOn = Instant.now, lastModified = Instant.now)))
  override def getCaseInstance(id: String): Future[Option[CaseInstance]] =  Future.successful(None)
  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFile]] = Future.successful(None)
}
