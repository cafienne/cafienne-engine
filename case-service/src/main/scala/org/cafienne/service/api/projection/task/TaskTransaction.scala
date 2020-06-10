package org.cafienne.service.api.projection.task

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.akka.event.CaseModified
import org.cafienne.humantask.akka.event._
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tasks.TaskRecord

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class TaskTransaction(taskId: String, persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {

  val tasks = scala.collection.mutable.HashMap[String, TaskRecord]()

  def handleEvent(evt: HumanTaskEvent): Future[Done] = {
    logger.debug("Handling event of type " + evt.getClass.getSimpleName + " on task " + taskId)

    evt match {
      case event: HumanTaskCreated => createTask(event)
      case event: HumanTaskEvent => handleHumanTaskEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  def createTask(evt: HumanTaskCreated): Future[Done] = {
    this.tasks.put(evt.taskId, TaskRecord(id = evt.taskId,
      caseInstanceId = evt.getActorId,
      tenant = evt.tenant,
      taskName = evt.getTaskName,
      createdOn = evt.getCreatedOn,
      createdBy = evt.getCreatedBy,
      lastModified = evt.getCreatedOn,
      modifiedBy = evt.getCreatedBy,
    ))
    Future.successful{ Done }
  }

  def updateLastModifiedInformationInTasks(event: CaseModified) = {
    tasks.values.foreach(task => tasks.put(task.id, TaskMerger(event, task)))
  }

  def handleHumanTaskEvent(event: HumanTaskEvent) = {
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
              case other => {
                System.err.println("We missed out on HumanTaskTransition event of type " + other.getClass.getName)
                copy
              }
            }
        }))
      }
    }


    fTask.map {
      case Some(task) => this.tasks.put(task.id, task)
      case _ => logger.error("Could not find task with id " + event.taskId + " in the current database. This may lead to problems. Ignoring event of type " + event.getClass.getName)
    }.flatMap(_ => Future.successful(Done))

  }

  def commit(offsetName: String, offset: Offset): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update
    var records = ListBuffer.empty[AnyRef]
    records ++= this.tasks.values

    // Even if there are no new records, we will still update the offset store
    records += OffsetRecord(offsetName, offset)

    persistence.bulkUpdate(records.filter(r => r != null))
  }

  private def fetchTask(taskId: String) = {
    this.tasks.get(taskId) match {
      case None =>
        logger.debug("Retrieving task " + taskId + " from database")
        persistence.getTask(taskId)
      case Some(task) => Future.successful(Some(task))
    }
  }
}
