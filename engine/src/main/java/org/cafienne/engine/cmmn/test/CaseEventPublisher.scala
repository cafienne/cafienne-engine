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

package org.cafienne.engine.cmmn.test

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.query.{EventEnvelope, Offset}
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.{Done, NotUsed}
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class CaseEventPublisher(listener: CaseEventListener, val caseSystem: CaseSystem) extends ReadJournalProvider {
  override val system: ActorSystem = caseSystem.system
  override val readJournal: String = caseSystem.config.persistence.readJournal
  val source: Source[EventEnvelope, NotUsed] = journal().eventsByTag(ModelEvent.TAG, Offset.noOffset)
  source.mapAsync(1) {
    case EventEnvelope(newOffset, persistenceId, sequenceNr, evt: AnyRef) => {
      listener.handle(evt)
      Future.successful(Done)
    }
    case _ => Future.successful(Done) // Ignore other events
  }.runWith(Sink.ignore)
}
