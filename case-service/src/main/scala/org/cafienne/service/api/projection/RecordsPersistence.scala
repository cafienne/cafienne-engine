package org.cafienne.service.api.projection

import akka.Done
import org.cafienne.service.api.cases.table.{CaseFileRecord, CaseRecord, PlanItemRecord}
import org.cafienne.service.api.tasks.TaskRecord

import scala.concurrent.Future

trait RecordsPersistence {
  def bulkUpdate(records: Seq[AnyRef]): Future[Done]

  def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]]

  def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]]

  def getCaseInstance(caseInstanceId: String): Future[Option[CaseRecord]]

  def getTask(taskId: String): Future[Option[TaskRecord]]
}
