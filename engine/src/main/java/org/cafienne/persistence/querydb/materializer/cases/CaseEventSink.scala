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

package org.cafienne.persistence.querydb.materializer.cases

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.persistence.querydb.materializer.{QueryDBEventSink, QueryDBStorage}

import scala.concurrent.Future

class CaseEventSink(override val system: ActorSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val tag: String = CaseEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(CaseEventSink.offsetName)

  override def createBatch(persistenceId: String): CaseEventBatch = new CaseEventBatch(this, persistenceId, storage)
}

object CaseEventSink {
  val offsetName = "CaseEventSink"
}

trait CaseEventMaterializer {
  val batch: CaseEventBatch
  lazy val caseInstanceId: String = batch.persistenceId
  lazy val dBTransaction: CaseStorageTransaction = batch.dBTransaction
  lazy val tenant: String = batch.tenant
}
