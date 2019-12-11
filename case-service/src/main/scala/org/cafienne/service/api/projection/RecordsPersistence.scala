package org.cafienne.service.api.projection

import akka.Done
import org.cafienne.service.api.cases.{CaseFile, CaseInstance, PlanItem}
import org.cafienne.service.api.tasks.Task

import scala.concurrent.Future

trait RecordsPersistence {
  def bulkUpdate(records: Seq[AnyRef]): Future[Done]

  def getPlanItem(planItemId: String): Future[Option[PlanItem]]

  def getCaseFile(caseInstanceId: String): Future[Option[CaseFile]]

  def getCaseInstance(caseInstanceId: String): Future[Option[CaseInstance]]

  def getTask(taskId: String): Future[Task]
}
