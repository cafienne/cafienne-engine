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
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.cqrs.batch.EventBatch

import java.time.Instant

class PublicCaseEventBatch(val persistenceId: String) extends EventBatch with LazyLogging {
  lazy val publicEvents: Seq[PublicEventWrapper] = {
    (CaseStarted.from(this)
      ++ StageCompleted.from(this)
      ++ HumanTaskCompleted.from(this)
      ++ HumanTaskTerminated.from(this)
      ++ MilestoneAchieved.from(this)
      ++ UserEventRaised.from(this)
      ++ StageActivated.from(this)
      ++ HumanTaskStarted.from(this)
      ++ MilestoneAvailable.from(this)
      ++ UserEventCreated.from(this)
      ++ CaseCompleted.from(this))
      .sortBy(_.sequenceNr)
  }
  lazy val timestamp: Instant = events.map(_.event).last.getTimestamp
  lazy val offset: Offset = events.last.offset

  def getSequenceNr(event: ModelEvent): Long = events.find(_.event == event).fold(throw new IllegalArgumentException("Cannot find event inside batch"))(_.sequenceNr)

  // Simple mechanism to filter out all events that have or extend a certain class of ModelEvent
  def filterMap[T <: ModelEvent](clazz: Class[T]): Seq[T] = events.map(_.event).filter(event => clazz.isAssignableFrom(event.getClass)).map(_.asInstanceOf[T]).toSeq

  def createPublicEvents: PublicCaseEventBatch = {
    logger.whenDebugEnabled(logger.debug(s"Batch on case [$persistenceId] at offset [$offset] has ${publicEvents.size} public events: ${publicEvents.map(_.manifest).mkString(", ")}"))
    publicEvents // This should lazily load the public events
    this
  }
}
