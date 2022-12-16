package org.cafienne.storage.actormodel
import org.cafienne.cmmn.actorapi.event.plan.{PlanItemCreated, PlanItemTransitioned}
import org.cafienne.cmmn.instance.PlanItemType

import scala.concurrent.Future

trait CaseChildrenFinder extends StorageActorState {
  override def findCascadingChildren(): Future[Seq[ActorMetadata]] = {
    def taskCreatedFinder(taskType: PlanItemType, finder: String => ActorMetadata): Seq[ActorMetadata] = {
      events
        .filter(_.isInstanceOf[PlanItemCreated])
        .map(_.asInstanceOf[PlanItemCreated])
        .filter(_.getType == taskType)
        .map(_.getPlanItemId).filter(taskMustBeActivated).map(finder).toSeq
    }

    def taskMustBeActivated(taskId: String): Boolean =
      events
        .filter(_.isInstanceOf[PlanItemTransitioned])
        .map(_.asInstanceOf[PlanItemTransitioned])
        .filter(_.getPlanItemId == taskId)
        .exists(_.getCurrentState.isActive)

    Future.successful({
      val cases = taskCreatedFinder(PlanItemType.CaseTask, metadata.caseMember)
      val processes = taskCreatedFinder(PlanItemType.ProcessTask, metadata.processMember)
      //      println(s"Found ${cases.length} cases and ${processes.length} processes: ${cases ++ processes}")
      cases ++ processes
    })
  }
}
