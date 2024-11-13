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

package com.casefabric.querydb.materializer.consentgroup

import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorSystem
import com.casefabric.consentgroup.actorapi.event.ConsentGroupEvent
import com.casefabric.querydb.materializer.{QueryDBEventSink, QueryDBStorage}
import com.casefabric.system.CaseSystem

import scala.concurrent.Future

class ConsentGroupEventSink(val caseSystem: CaseSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val system: ActorSystem = caseSystem.system

  override val tag: String = ConsentGroupEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(ConsentGroupEventSink.offsetName)

  override def createBatch(persistenceId: String): ConsentGroupEventBatch = new ConsentGroupEventBatch(this, persistenceId, storage)
}

object ConsentGroupEventSink {
  val offsetName = "ConsentGroupEventSink"
}

trait ConsentGroupEventMaterializer {
  val batch: ConsentGroupEventBatch
  lazy val groupId: String = batch.persistenceId
  lazy val dBTransaction: ConsentGroupStorageTransaction = batch.dBTransaction
}
