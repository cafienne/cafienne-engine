/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.cmmn.actorapi.event.file.CaseFileEvent
import org.cafienne.cmmn.actorapi.event.plan.task.TaskEvent
import org.cafienne.cmmn.actorapi.event.plan.{CasePlanEvent, PlanItemEvent}
import org.cafienne.cmmn.actorapi.event.team.CaseTeamEvent
import org.cafienne.humantask.actorapi.event.HumanTaskEvent
import org.cafienne.infrastructure.cqrs.batch.EventBatch

import java.time.Instant

class PublicCaseEventBatch(val persistenceId: String) extends EventBatch with LazyLogging {
  lazy val caseEvents: Seq[CaseEvent] = events.map(_.event).filter(_.isInstanceOf[CaseEvent]).map(_.asInstanceOf[CaseEvent]).toSeq
  lazy val caseFileEvents: Seq[CaseFileEvent] = filterMap(classOf[CaseFileEvent])
  lazy val caseTeamEvents: Seq[CaseTeamEvent] = filterMap(classOf[CaseTeamEvent])
  lazy val casePlanEvents: Seq[CasePlanEvent[_]] = filterMap(classOf[CasePlanEvent[_]])
  lazy val planItemEvents: Seq[PlanItemEvent] = filterMap(classOf[PlanItemEvent])
  lazy val taskEvents: Seq[TaskEvent[_]] = filterMap(classOf[TaskEvent[_]])
  lazy val humanTaskEvents: Seq[HumanTaskEvent] = filterMap(classOf[HumanTaskEvent])
  lazy val userEventEvents: Seq[PlanItemEvent] = planItemEvents.filter(_.getType.equals("UserEvent"))
  lazy val milestoneEvents: Seq[PlanItemEvent] = planItemEvents.filter(_.getType.equals("Milestone"))
  lazy val publicEvents: Seq[PublicEventWrapper] = {
    (Seq()
      // Note, order sort of matters. Typically, case started leads to events like HumanTaskStarted.
      //  But, also HumanTaskCompleted leads to new tasks getting started.
      //  Case Completed always comes at the end (we assume ;)
      ++ CaseStarted.from(this)
      ++ HumanTaskCompleted.from(this)
      ++ HumanTaskTerminated.from(this)
      ++ MilestoneAchieved.from(this)
      ++ UserEventRaised.from(this)
      ++ HumanTaskStarted.from(this)
      ++ MilestoneAvailable.from(this)
      ++ UserEventCreated.from(this)
      ++ CaseCompleted.from(this))
      // Wrap all public events and add a commit event with offset information
      .map(event => new PublicEventWrapper(this.timestamp, event))
  }
  lazy val timestamp: Instant = caseEvents.last.getTimestamp
  lazy val offset: Offset = events.last.offset

  def filterMap[T <: CaseEvent](clazz: Class[T]): Seq[T] = caseEvents.filter(event => clazz.isAssignableFrom(event.getClass)).map(_.asInstanceOf[T])

  def createPublicEvents: PublicCaseEventBatch = {
    logger.whenDebugEnabled(logger.debug(s"Batch on case [$persistenceId] at offset [$offset] has ${publicEvents.size} public events: ${publicEvents.map(_.manifest).mkString(", ")}"))
    publicEvents // This should lazily load the public events
    this
  }
}
