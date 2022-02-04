package org.cafienne.service.db.materializer.cases.plan

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseModified
import org.cafienne.cmmn.actorapi.event.migration.{PlanItemDropped, PlanItemMigrated}
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.humantask.actorapi.event._
import org.cafienne.humantask.actorapi.event.migration.{HumanTaskDropped, HumanTaskMigrated}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record._

import scala.concurrent.{ExecutionContext, Future}

class CasePlanProjection(persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {

  private val planItems = scala.collection.mutable.HashMap[String, PlanItemRecord]()
  private val planItemsHistory = scala.collection.mutable.Buffer[PlanItemHistoryRecord]()
  private val tasks = scala.collection.mutable.HashMap[String, TaskRecord]()

  def handleCasePlanEvent(event: CasePlanEvent[_]): Future[Done] = {
    event match {
      case event: PlanItemEvent => handlePlanItemEvent(event)
      case event: HumanTaskCreated => deprecatedCreateTask(event)
      case event: HumanTaskActivated => createTask(event)
      case event: HumanTaskEvent => handleHumanTaskEvent(event)
      case _ => Future.successful(Done) // ignore other events (e.g. TaskInputFilled and TaskOutputFilled)
    }
  }

  private def handlePlanItemEvent(event: PlanItemEvent): Future[Done] = {
    event match {
      case dropped: PlanItemDropped =>
        persistence.deletePlanItemRecordAndHistory(dropped.getPlanItemId)
        Future.successful(Done)
      case _ =>
        // Always insert new items into history, no need to first fetch them from db.
        PlanItemHistoryMerger.mapEventToHistory(event).foreach(item => planItemsHistory += item)
        event match {
          case evt: PlanItemCreated =>
            val planItem = PlanItemMerger.merge(evt)
            planItems.put(planItem.id, planItem)
            Future.successful(Done)
          case other: PlanItemEvent => getPlanItem(event.getPlanItemId).map {
            case Some(planItem) =>
              other match {
                case evt: PlanItemTransitioned => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case evt: RepetitionRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case evt: RequiredRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case evt: PlanItemMigrated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case _ => // Nothing to do for the other events
              }
              Done
            case None =>
              // But ... if not found, then should we create a new one here? With the PlanItemMerger that can be done ...
              logger.error("Expected PlanItem " + event.getPlanItemId + " in " + event.getCaseInstanceId + ", but not found in the database on event type " + event.getClass.getSimpleName)
              Done
          }
          case unknownPlanItemEvent =>
            logger.error("Apparently we have a new type of PlanItemEvent that is not being handled by this Projection. The type is " + unknownPlanItemEvent.getClass.getName)
            Future.successful(Done)
        }
    }
  }

  private def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]] = {
    planItems.get(planItemId) match {
      case Some(item) =>
        logger.whenDebugEnabled(logger.debug(s"Found plan item $planItemId in current transaction cache"))
        Future.successful(Some(item))
      case None =>
        logger.whenDebugEnabled(logger.debug(s"Retrieving plan item $planItemId from database"))
        persistence.getPlanItem(planItemId)
    }
  }

  private def createTask(evt: HumanTaskActivated): Future[Done] = {
    // See above comments. HumanTaskActivated has replaced HumanTaskCreated.
    //  We check here to see if our version is an old or a new one, by checking whether
    //  a task is already available in the transaction (that means HumanTaskCreated was still there, the old format).
    val updatedTask = this.tasks.get(evt.taskId) match {
      case None => TaskMerger.create(evt) // New format. TaskMerger will create the task
      case Some(task) => TaskMerger(evt, task) // Old format, must have been created in same transaction through HumanTaskCreated, fine too
    }
    this.tasks.put(evt.taskId, updatedTask)
    Future.successful(Done)
  }

  private def deprecatedCreateTask(evt: HumanTaskCreated): Future[Done] = {
    this.tasks.put(evt.taskId, TaskMerger.create(evt))
    Future.successful(Done)
  }

  private def handleHumanTaskEvent(event: HumanTaskEvent): Future[Done] = {
    event match {
      case dropped: HumanTaskDropped =>
        persistence.deleteTaskRecord(dropped.taskId)
        return Future.successful(Done)
      case _ =>
    }

    val fTask: Future[Option[TaskRecord]] = {
      event match {
        case evt: HumanTaskInputSaved => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskOutputSaved => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskOwnerChanged => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskDueDateFilled => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskTransitioned => fetchTask(event.taskId).map(task => task.map(t => {
          val copy = TaskMerger(evt, t)
          evt match {
            case evt: HumanTaskAssigned => TaskMerger(evt, copy)
            case evt: HumanTaskActivated => TaskMerger(evt, copy)
            case evt: HumanTaskCompleted => TaskMerger(evt, copy)
            case evt: HumanTaskTerminated => TaskMerger(evt, copy)
            case other => copy // No need to do any further updates to the task record
          }
        }))
        case evt: HumanTaskMigrated => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case _ => Future.successful(None) // Ignore and error on other events
      }
    }

    fTask.map {
      case Some(task) => this.tasks.put(task.id, task)
      case _ => logger.error(s"Could not find task '${event.getTaskName}' with id ${event.taskId} in the current database. This may lead to problems. Ignoring event of type ${event.getClass.getName}")
    }.flatMap(_ => Future.successful(Done))
  }

  private def fetchTask(taskId: String): Future[Option[TaskRecord]] = {
    this.tasks.get(taskId) match {
      case None =>
        logger.whenDebugEnabled(logger.debug("Retrieving task " + taskId + " from database"))
        persistence.getTask(taskId)
      case Some(task) => Future.successful(Some(task))
    }
  }

  def prepareCommit(caseModified: CaseModified): Unit = {
    // Add lastModified field to plan items and tasks
    this.planItems.values.map(item => PlanItemMerger.merge(caseModified, item)).foreach(item => persistence.upsert(item))
    this.planItemsHistory.map(item => PlanItemHistoryMerger.merge(caseModified, item)).foreach(item => persistence.upsert(item))
    this.tasks.values.map(current => TaskMerger(caseModified, current)).foreach(item => persistence.upsert(item))
  }
}
