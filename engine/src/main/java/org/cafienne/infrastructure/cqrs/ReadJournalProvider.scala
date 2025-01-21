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

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.query.PersistenceQuery
import org.apache.pekko.persistence.query.scaladsl._
import org.cafienne.infrastructure.Cafienne

/**
  * Provides all query types of ReadJournal (eventsByTag, eventsById, etc.)
  */
trait ReadJournalProvider extends LazyLogging {
  def system: ActorSystem
  implicit def actorSystem: ActorSystem = system

  /**
    * Provides the requested journal
    *
    * @return
    */
  def journal(): ReadJournal with CurrentPersistenceIdsQuery with EventsByTagQuery with CurrentEventsByTagQuery with EventsByPersistenceIdQuery with CurrentEventsByPersistenceIdQuery = {
    PersistenceQuery(system).readJournalFor[ReadJournal with CurrentPersistenceIdsQuery with EventsByTagQuery with CurrentEventsByTagQuery with EventsByPersistenceIdQuery with CurrentEventsByPersistenceIdQuery](Cafienne.config.persistence.readJournal)
  }

}
