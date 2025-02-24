/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.materializer.cases.plan

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseModified
import org.cafienne.cmmn.actorapi.event.migration.{PlanItemDropped, PlanItemMigrated}
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.humantask.actorapi.event._
import org.cafienne.humantask.actorapi.event.migration.{HumanTaskDropped, HumanTaskMigrated}
import org.cafienne.persistence.querydb.materializer.cases.{CaseEventBatch, CaseEventMaterializer}
import org.cafienne.persistence.querydb.record.{PlanItemRecord, TaskRecord}

class CasePlanProjection(override val batch: CaseEventBatch) extends CaseEventMaterializer with LazyLogging {

  private val planItems = scala.collection.mutable.HashMap[String, PlanItemRecord]()
  private val tasks = scala.collection.mutable.HashMap[String, TaskRecord]()

  def handleCasePlanEvent(event: CasePlanEvent): Unit = {
    event match {
      case event: HumanTaskCreated => deprecatedCreateTask(event)
      case event: HumanTaskActivated => createTask(event)
      case event: HumanTaskEvent => handleHumanTaskEvent(event)
      case event: CasePlanEvent => handlePlanItemEvent(event)
      case _ => // ignore other events (e.g. TaskInputFilled and TaskOutputFilled)
    }
  }

  private def handlePlanItemEvent(event: CasePlanEvent): Unit = {
    event match {
      case dropped: PlanItemDropped =>
        dBTransaction.deletePlanItemRecord(dropped.getPlanItemId)
      case _ =>
        event match {
          case evt: PlanItemCreated =>
            val planItem = PlanItemMerger.merge(evt)
            planItems.put(planItem.id, planItem)
          case other: CasePlanEvent => getPlanItem(event.getPlanItemId).map {
            case planItem: PlanItemRecord =>
              other match {
                case evt: PlanItemTransitioned => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case evt: RepetitionRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case evt: RequiredRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case evt: PlanItemMigrated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
                case _ => // Nothing to do for the other events
              }
            case another =>
              // But ... if not found, then should we create a new one here? With the PlanItemMerger that can be done ...
              logger.error("Got " + another + " event, but PlanItem " + event.getPlanItemId + " in " + event.getCaseInstanceId + "not found in the database on event type " + event.getClass.getSimpleName)

          }
          case unknownCasePlanEvent =>
            logger.error("Apparently we have a new type of CasePlanEvent that is not being handled by this Projection. The type is " + unknownCasePlanEvent.getClass.getName)
        }
    }
  }

  private def getPlanItem(planItemId: String): Option[PlanItemRecord] = {
    planItems.get(planItemId) match {
      case Some(item) =>
        logger.whenDebugEnabled(logger.debug(s"Found plan item $planItemId in current transaction cache"))
        Some(item)
      case None =>
        logger.whenDebugEnabled(logger.debug(s"Retrieving plan item $planItemId from database"))
        dBTransaction.getPlanItem(planItemId)
    }
  }

  private def createTask(evt: HumanTaskActivated): Unit = {
    // See above comments. HumanTaskActivated has replaced HumanTaskCreated.
    //  We check here to see if our version is an old or a new one, by checking whether
    //  a task is already available in the transaction (that means HumanTaskCreated was still there, the old format).
    val updatedTask = this.tasks.get(evt.getTaskId) match {
      case None => TaskMerger.create(evt) // New format. TaskMerger will create the task
      case Some(task) => TaskMerger(evt, task) // Old format, must have been created in same transaction through HumanTaskCreated, fine too
    }
    this.tasks.put(evt.getTaskId, updatedTask)
  }

  private def deprecatedCreateTask(evt: HumanTaskCreated): Unit = {
    this.tasks.put(evt.getTaskId, TaskMerger.create(evt))
  }

  private def handleHumanTaskEvent(event: HumanTaskEvent): Unit = {
    event match {
      case dropped: HumanTaskDropped => dBTransaction.deleteTaskRecord(dropped.getTaskId) // No need trying to fetch
      case _ =>
        // Fetch the task
        val fTask: Option[TaskRecord] = {
          event match {
            case evt: HumanTaskInputSaved => fetchTask(event.getTaskId).map(task => TaskMerger(evt, task))
            case evt: HumanTaskOutputSaved => fetchTask(event.getTaskId).map(task => TaskMerger(evt, task))
            case evt: HumanTaskOwnerChanged => fetchTask(event.getTaskId).map(task => TaskMerger(evt, task))
            case evt: HumanTaskDueDateFilled => fetchTask(event.getTaskId).map(task => TaskMerger(evt, task))
            case evt: HumanTaskTransitioned => fetchTask(event.getTaskId).map(task => {
              val copy = TaskMerger(evt, task)
              evt match {
                case evt: HumanTaskAssigned => TaskMerger(evt, copy)
                case evt: HumanTaskActivated => TaskMerger(evt, copy)
                case evt: HumanTaskCompleted => TaskMerger(evt, copy)
                case evt: HumanTaskTerminated => TaskMerger(evt, copy)
                case other => copy // No need to do any further updates to the task record
              }
            })
            case evt: HumanTaskMigrated => fetchTask(event.getTaskId).map(task => TaskMerger(evt, task))
            case _ => None // Ignore and error on other events
          }
        }

        fTask match {
          case Some(task) => this.tasks.put(task.id, task)
          case None => logger.error(s"Could not find task '${event.getTaskName}' with id ${event.getTaskId} in the current database. This may lead to problems. Ignoring event of type ${event.getClass.getName}")
        }
    }
  }

  private def fetchTask(taskId: String): Option[TaskRecord] = {
    this.tasks.get(taskId) match {
      case None =>
        logger.whenDebugEnabled(logger.debug("Retrieving task " + taskId + " from database"))
        dBTransaction.getTask(taskId)
      case Some(task) => Some(task)
    }
  }

  def prepareCommit(caseModified: CaseModified): Unit = {
    // Add lastModified field to plan items and tasks
    this.planItems.values.map(item => PlanItemMerger.merge(caseModified, item)).foreach(item => dBTransaction.upsert(item))
    this.tasks.values.map(current => TaskMerger(caseModified, current)).foreach(item => dBTransaction.upsert(item))
  }
}
