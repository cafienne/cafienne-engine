package org.cafienne.service.db.materializer.cases

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.TransactionEvent
import org.cafienne.cmmn.actorapi.event._
import org.cafienne.cmmn.actorapi.event.file._
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.cmmn.actorapi.event.team._
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.cases.file.CaseFileProjection
import org.cafienne.service.db.materializer.cases.plan.CasePlanProjection
import org.cafienne.service.db.materializer.cases.team.CaseTeamProjection
import org.cafienne.service.db.materializer.slick.SlickTransaction

import scala.concurrent.{ExecutionContext, Future}

class CaseTransaction
  (caseInstanceId: String, tenant: String, persistence: RecordsPersistence, offsetStorage: OffsetStorage)
  (implicit val executionContext: ExecutionContext) extends SlickTransaction with LazyLogging {

  private val caseTeamProjection = new CaseTeamProjection(persistence)
  private val caseFileProjection = new CaseFileProjection(persistence, caseInstanceId, tenant)
  private val casePlanProjection = new CasePlanProjection(persistence)
  private val caseProjection = new CaseProjection(persistence, caseFileProjection)

  override def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    logger.whenDebugEnabled(logger.debug("Handling event of type " + envelope.event.getClass.getSimpleName + " in case " + caseInstanceId))
    envelope.event match {
      case event: CasePlanEvent[_] => casePlanProjection.handleCasePlanEvent(event)
      case event: CaseFileEvent => caseFileProjection.handleCaseFileEvent(event)
      case event: CaseTeamEvent => caseTeamProjection.handleCaseTeamEvent(event)
      case event: CaseAppliedPlatformUpdate => updateUserIds(event, envelope)
      case event: CaseEvent => caseProjection.handleCaseEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  private def updateUserIds(event: CaseAppliedPlatformUpdate, envelope: ModelEventEnvelope): Future[Done] = {
    persistence.updateCaseUserInformation(event.getCaseInstanceId, event.newUserInformation.info, offsetStorage.createOffsetRecord(envelope.offset))
  }

  override def commit(envelope: ModelEventEnvelope, caseModified: TransactionEvent[_]): Future[Done] = {
    // Tell the projections to prepare for commit, i.e. let them update the persistence.
    caseProjection.prepareCommit()
    caseTeamProjection.prepareCommit()
    caseFileProjection.prepareCommit()
    casePlanProjection.prepareCommit(caseModified)

    // Update the offset of the last event handled in this projection
    persistence.upsert(offsetStorage.createOffsetRecord(envelope.offset))

    persistence.commit()
  }
}
