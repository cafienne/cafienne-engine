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

package org.cafienne.service.akkahttp.debug

import org.apache.pekko.persistence.query.EventEnvelope
import org.apache.pekko.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.json.{ValueList, ValueMap}
import org.cafienne.system.CaseSystem

import scala.concurrent.{ExecutionContextExecutor, Future}

class ModelEventsReader(val caseSystem: CaseSystem) extends LazyLogging with ReadJournalProvider {
  override val system = caseSystem.system

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def getEvents(user: PlatformUser, actorId: String, from: Long, to: Long): Future[ValueList] = {
    val eventList = new ValueList
    val source: Source[EventEnvelope, org.apache.pekko.NotUsed] = journal().currentEventsByPersistenceId(actorId, from, to)
    source.runForeach {
      case EventEnvelope(offset, _, sequenceNr: Long, event: ModelEvent) =>
        if (user == null || user.tenants.contains(event.tenant) || user.isPlatformOwner) {
          val eventNr = sequenceNr.asInstanceOf[java.lang.Long]
          val eventType = event.getClass.getSimpleName
          eventList.add(new ValueMap("nr", eventNr, "offset", OffsetRecord("", offset).offsetValue, "type", eventType, "content", event.rawJson))
        }
      case _ => {
        // ignoring other events
      }
    }.flatMap{ _ =>
      Future{eventList}
    }
  }
}
