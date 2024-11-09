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

package org.cafienne.infrastructure.cqrs

import org.apache.pekko.NotUsed
import org.apache.pekko.persistence.query.EventEnvelope
import org.apache.pekko.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging

/**
  * Provides a Source of ModelEvents with the events of a specific persistence id (typically a case or a tenant or a consent group)
  */
trait InstanceEventSource extends ReadJournalProvider with ModelEventFilter with LazyLogging  {
  /**
    * Query to retrieve the events as Source.
    * Defaults to journal.currentEventsByPersistenceId, overrides can alternatively use journal.eventsByTag
    * for a livestream.
    * @param offset
    * @return
    */
  def query(actorId: String): Source[EventEnvelope, NotUsed] = journal().currentEventsByPersistenceId(actorId, 0L, Long.MaxValue)

  /**
    * Composes the Source
    */
  def events(actorId: String): Source[ModelEventEnvelope, NotUsed] =
    query(actorId)
      .filter(validateModelEvents) // Only interested in ModelEvents, but we log errors if it is an unexpected event or deserialization issue
      .map(ModelEventEnvelope) // Construct a simple wrapper that understands we're dealing with ModelEvents
}
