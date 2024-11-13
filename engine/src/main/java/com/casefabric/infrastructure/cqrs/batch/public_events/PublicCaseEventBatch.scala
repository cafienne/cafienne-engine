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

package com.casefabric.infrastructure.cqrs.batch.public_events

import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.actormodel.event.ModelEvent
import com.casefabric.infrastructure.cqrs.batch.EventBatch
import com.casefabric.infrastructure.cqrs.batch.public_events.migration.{CaseMigrated, HumanTaskDropped, HumanTaskMigrated, MilestoneDropped, MilestoneMigrated, StageDropped, StageMigrated}

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
      ++ MilestonePending.from(this)
      ++ UserEventCreated.from(this)
      ++ CaseMigrated.from(this)
      ++ HumanTaskMigrated.from(this)
      ++ HumanTaskDropped.from(this)
      ++ StageMigrated.from(this)
      ++ StageDropped.from(this)
      ++ MilestoneMigrated.from(this)
      ++ MilestoneDropped.from(this)
      ++ CaseCompleted.from(this))
      .sortBy(_.sequenceNr)
  }
  lazy val timestamp: Instant = events.map(_.event).last.getTimestamp
  lazy val offset: Offset = events.last.offset

  def getSequenceNr(event: ModelEvent): Long = events.find(_.event == event).fold(throw new IllegalArgumentException("Cannot find event inside batch"))(_.sequenceNr)

  // Simple mechanism to filter out all events that have or extend a certain class of ModelEvent
  def filterMap[ME <: ModelEvent](clazz: Class[ME]): Seq[ME] = events.map(_.event).filter(event => clazz.isAssignableFrom(event.getClass)).map(_.asInstanceOf[ME]).toSeq

  def publicEvents[PE <: CaseFabricPublicEventContent](clazz: Class[PE]): Seq[PE] = publicEvents.map(_.content).filter(event => clazz.isAssignableFrom(event.getClass)).map(_.asInstanceOf[PE])

  def createPublicEvents: PublicCaseEventBatch = {
    logger.whenDebugEnabled(logger.debug(s"Batch on case [$persistenceId] at offset [$offset] has ${publicEvents.size} public events: ${publicEvents.map(_.manifest).mkString(", ")}"))
    publicEvents // This should lazily load the public events
    this
  }
}
