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

package org.cafienne.querydb.materializer.cases

import org.apache.pekko.Done
import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.cmmn.actorapi.event.file.CaseFileEvent
import org.cafienne.cmmn.actorapi.event.{CaseAppliedPlatformUpdate, CaseEvent, CaseModified}
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent
import org.cafienne.cmmn.actorapi.event.team.CaseTeamEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.cases.file.CaseFileProjection
import org.cafienne.querydb.materializer.cases.plan.CasePlanProjection
import org.cafienne.querydb.materializer.cases.team.CaseTeamProjection
import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}

import scala.concurrent.Future

class CaseEventBatch(val sink: CaseEventSink, override val persistenceId: String, val storage: QueryDBStorage) extends QueryDBEventBatch with LazyLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  val caseInstanceId: String = persistenceId
  lazy val tenant: String = events.head.event.tenant()
  val dBTransaction: CaseStorageTransaction = storage.createCaseTransaction(persistenceId)

  private val caseTeamProjection = new CaseTeamProjection(this)
  private val caseFileProjection = new CaseFileProjection(this)
  private val casePlanProjection = new CasePlanProjection(this)
  private val caseProjection = new CaseProjection(this, caseFileProjection)

  def createOffsetRecord(offset: Offset): OffsetRecord = OffsetRecord(CaseEventSink.offsetName, offset)

  override def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    logger.whenDebugEnabled(logger.debug("Handling event of type " + envelope.event.getClass.getSimpleName + " in case " + caseInstanceId))
    envelope.event match {
      case event: CasePlanEvent => casePlanProjection.handleCasePlanEvent(event)
      case event: CaseFileEvent => caseFileProjection.handleCaseFileEvent(event)
      case event: CaseTeamEvent => caseTeamProjection.handleCaseTeamEvent(event)
      case event: CaseEvent => caseProjection.handleCaseEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case caseModified: CaseModified => commitCaseRecords(envelope, caseModified)
      case event: CaseAppliedPlatformUpdate => updateUserIds(event, envelope)
      case _ =>
        logger.warn(s"CaseTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitCaseRecords(envelope: ModelEventEnvelope, caseModified: CaseModified): Future[Done] = {
    // Tell the projections to prepare for commit, i.e. let them update the persistence.
    caseProjection.prepareCommit()
    caseTeamProjection.prepareCommit()
    caseFileProjection.prepareCommit()
    casePlanProjection.prepareCommit(caseModified)
    // Update the offset storage with the latest & greatest offset we handled
    dBTransaction.upsert(createOffsetRecord(envelope.offset))

    // Commit and then inform the last modified registration
    dBTransaction.commit().andThen(_ => CaseReader.lastModifiedRegistration.handle(caseModified))
  }

  private def updateUserIds(event: CaseAppliedPlatformUpdate, envelope: ModelEventEnvelope): Future[Done] = {
    dBTransaction.updateCaseUserInformation(event.getCaseInstanceId, event.newUserInformation.info, createOffsetRecord(envelope.offset))
  }
}
