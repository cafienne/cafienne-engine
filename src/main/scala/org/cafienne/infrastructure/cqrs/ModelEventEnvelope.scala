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

import akka.persistence.query.{EventEnvelope, Offset}
import org.cafienne.actormodel.event.ModelEvent

case class ModelEventEnvelope(envelope: EventEnvelope) {
  lazy val offset: Offset = envelope.offset
  lazy val persistenceId: String = envelope.persistenceId
  lazy val sequenceNr: Long = envelope.sequenceNr
  lazy val event: ModelEvent = envelope.event.asInstanceOf[ModelEvent]
}

