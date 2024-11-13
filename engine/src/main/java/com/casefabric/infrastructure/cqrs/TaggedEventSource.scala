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

package com.casefabric.infrastructure.cqrs

import org.apache.pekko.NotUsed
import org.apache.pekko.persistence.query.{EventEnvelope, Offset}
import org.apache.pekko.stream.scaladsl.{RestartSource, Source}
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.system.health.HealthMonitor

import scala.concurrent.Future

/**
  * Provides a Source of ModelEvents having a certain Tag (wrapped in an envelope)
  * Reads those events from the given offset onwards.
  */
trait TaggedEventSource extends ReadJournalProvider with ModelEventFilter with LazyLogging  {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Provide the offset from which we should start sourcing events
    */
  def getOffset: Future[Offset]

  /**
    * Tag to scan events for
    */
  val tag: String

  /**
    * Query to retrieve the events as Source.
    * Defaults to journal.eventsByTag, overrides can alternatively use journal.currentEventsByTag
    * @param offset
    * @return
    */
  def query(offset: Offset): Source[EventEnvelope, NotUsed] = journal().eventsByTag(tag, offset)

  /**
    * Returns the Source
    */
  def taggedEvents: Source[ModelEventEnvelope, NotUsed] =
    restartableTaggedEventSourceFromLastKnownOffset
      .map(reportHealth) // The fact that we receive an event here is an indication that the readJournal is healthy
      .filter(validateModelEvents) // Only interested in ModelEvents, but we log errors if it is an unexpected event or deserialization issue
      .map(ModelEventEnvelope) // Construct a simple wrapper that understands we're dealing with ModelEvents

  def restartableTaggedEventSourceFromLastKnownOffset: Source[EventEnvelope, NotUsed] = {
    RestartSource.withBackoff(CaseFabric.config.persistence.queryDB.restartSettings) { () =>
      Source.futureSource({
        // First read the last known offset, then get return the events by tag from that offset onwards.
        //  Note: when the source restarts, it will freshly fetch the last known offset, thereby avoiding
        //  consuming that were consumed already successfully before the source had to be restarted.
        getOffset.map { offset: Offset =>
          logger.warn(s"Starting to read '$tag' events from offset " + offset)
          query(offset)
        }
      })
    }
  }

  /**
    * Identity function that has side-effect to indicate a healthy read journal
    */
  def reportHealth(envelope: EventEnvelope): EventEnvelope = {
    // The fact that we receive an event here is an indication that the readJournal is healthy
    HealthMonitor.readJournal.isOK()
    envelope
  }
}
