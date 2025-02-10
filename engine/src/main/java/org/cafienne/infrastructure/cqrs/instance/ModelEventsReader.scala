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

package org.cafienne.infrastructure.cqrs.instance

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorSystem
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContextExecutor, Future}

class ModelEventsReader(val caseSystem: CaseSystem) extends InstanceEventSource with LazyLogging {
  override val system: ActorSystem = caseSystem.system
  override val readJournal: String = caseSystem.config.persistence.readJournal

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def getEvents(user: PlatformUser, actorId: String, from: Long, to: Long): Future[Seq[PublicModelEvent]] = {
    val eventList = new ListBuffer[PublicModelEvent]
    events(actorId, from, to).runForeach(envelope => {
      val event = envelope.event
      if (user == null || user.tenants.contains(event.tenant) || user.isPlatformOwner) {
        eventList += new PublicModelEvent(envelope.sequenceNr, envelope.offset, event)
      }
    }).map(_ => eventList.toSeq)
  }
}
