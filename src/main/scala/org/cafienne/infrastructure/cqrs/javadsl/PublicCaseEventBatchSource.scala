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

package org.cafienne.infrastructure.cqrs.javadsl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.Offset
import akka.stream.javadsl
import org.cafienne.infrastructure.cqrs.batch.public_events.PublicCaseEventBatch

import scala.concurrent.Future

class PublicCaseEventBatchSource(system: ActorSystem, offsetStorage: OffsetStorage) {

  private class Wrapper(override val system: ActorSystem, offsetStorage: OffsetStorage) extends org.cafienne.infrastructure.cqrs.batch.PublicCaseEventBatchSource {
    override def getOffset: Future[Offset] = offsetStorage.getOffset
  }

  private val wrapper = new Wrapper(system, offsetStorage)

  /**
    * Get a stream (Source) of public Events for java
    *
    * @return Source of PublicCaseEventBatches
    */
  def publicEvents(): javadsl.Source[PublicCaseEventBatch, NotUsed] = wrapper.publicEvents.asJava

}
