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

package org.cafienne.querydb.materializer.board

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.board.actorapi.event.BoardEvent
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBStorage}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class BoardEventSink(val caseSystem: CaseSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val system: ActorSystem = caseSystem.system

  override val tag: String = BoardEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(BoardEventSink.offsetName)

  override def createBatch(persistenceId: String): BoardEventBatch = new BoardEventBatch(this, persistenceId, storage)
}

object BoardEventSink {
  val offsetName = "BoardEventSink"
}
