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

package org.cafienne.querydb.materializer.tenant

import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorSystem
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBStorage}
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.event.TenantEvent

import scala.concurrent.Future

class TenantEventSink(val caseSystem: CaseSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val system: ActorSystem = caseSystem.system

  override val tag: String = TenantEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(TenantEventSink.offsetName)

  override def createBatch(persistenceId: String): TenantEventBatch = new TenantEventBatch(this, persistenceId, storage)
}

object TenantEventSink {
  val offsetName = "TenantEventSink"
}

trait TenantEventMaterializer {
  val batch: TenantEventBatch
  lazy val tenant: String = batch.persistenceId
  lazy val dBTransaction: TenantStorageTransaction = batch.dBTransaction
}