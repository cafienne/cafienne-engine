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

package org.cafienne.querydb.materializer.consentgroup

import org.apache.pekko.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.consentgroup.actorapi.event.{ConsentGroupCreated, ConsentGroupMemberEvent, ConsentGroupModified}
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}

import scala.concurrent.Future

class ConsentGroupEventBatch(val sink: ConsentGroupEventSink, override val persistenceId: String, storage: QueryDBStorage) extends QueryDBEventBatch with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global
  val dBTransaction: ConsentGroupStorageTransaction = storage.createConsentGroupTransaction(persistenceId)

  private val groupProjection = new GroupProjection(this)
  private val memberProjection = new GroupMemberProjection(this)

  def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    envelope.event match {
      case event: ConsentGroupCreated => groupProjection.handleGroupEvent(event)
      case event: ConsentGroupMemberEvent => memberProjection.handleMemberEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case event: ConsentGroupModified => commitGroupRecords(envelope, event)
      case _ =>
        logger.warn(s"ConsentGroupTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitGroupRecords(envelope: ModelEventEnvelope, event: ConsentGroupModified): Future[Done] = {
    // Add group and members (if any) to the db transaction
    groupProjection.prepareCommit()
    memberProjection.prepareCommit()
    // Update the offset of the last event handled in this projection
    dBTransaction.upsert(OffsetRecord(ConsentGroupEventSink.offsetName, envelope.offset))
    // Commit and then inform the last modified registration
    dBTransaction.commit().andThen(_ => {
      memberProjection.affectedUserIds.foreach(sink.caseSystem.userCache.clear)
      ConsentGroupReader.lastModifiedRegistration.handle(event)
    })
  }
}
