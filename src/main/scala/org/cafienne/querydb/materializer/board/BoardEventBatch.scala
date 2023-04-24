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

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.board.actorapi.event.definition.BoardDefinitionUpdated
import org.cafienne.board.actorapi.event.{BoardCreated, BoardEvent, BoardModified}
import org.cafienne.board.state.team.BoardTeam
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.cases.CaseReader
import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}
import org.cafienne.querydb.record.BoardRecord

import scala.concurrent.Future

class BoardEventBatch(val sink: BoardEventSink, override val persistenceId: String, storage: QueryDBStorage) extends QueryDBEventBatch with LazyLogging {
  val dBTransaction: BoardStorageTransaction = storage.createBoardTransaction(persistenceId)

  def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    envelope.event match {
      case event: BoardCreated => upsertBoard(event, event.title)
      case event: BoardDefinitionUpdated => upsertBoard(event, event.title)
      case _ =>
        Future.successful(Done) // Ignore other events
    }
  }

  def upsertBoard(event: BoardEvent, title: String): Future[Done] = {
    val boardId = event.getActorId
    val teamId = boardId + BoardTeam.EXTENSION
    val tenant = event.tenant()
    dBTransaction.upsert(BoardRecord(id = boardId, title = title, tenant = tenant, team = teamId))
    Future.successful(Done)
  }

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case event: BoardModified => commitBoardRecords(envelope, event)
      case _ =>
        logger.warn(s"BoardTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitBoardRecords(envelope: ModelEventEnvelope, event: BoardModified): Future[Done] = {
    // Add group and members (if any) to the db transaction
    // Update the offset of the last event handled in this projection
    dBTransaction.upsert(OffsetRecord(BoardEventSink.offsetName, envelope.offset))
    // Commit and then inform the last modified registration
    import scala.concurrent.ExecutionContext.Implicits.global
    // Update CaseReader's last modified registration, instead of BoardReader ...
    // TODO: enable BoardReader usage, but then also CaseReader is required for flow updates....
    dBTransaction.commit().andThen(_ => CaseReader.lastModifiedRegistration.handle(event))
  }
}
