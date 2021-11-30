package org.cafienne.service.db.materializer.tenant

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.service.api.tenant.TenantReader
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.slick.SlickTransaction
import org.cafienne.tenant.actorapi.event._
import org.cafienne.tenant.actorapi.event.platform.PlatformEvent

import scala.concurrent.{ExecutionContext, Future}

class TenantTransaction(tenant: String, persistence: RecordsPersistence, userCache: IdentityProvider, offsetStorage: OffsetStorage)
                       (implicit val executionContext: ExecutionContext) extends SlickTransaction with LazyLogging {

  private val tenantProjection = new TenantProjection(persistence)
  private val userProjection = new UserProjection(persistence)

  override def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    logger.debug("Handling event of type " + envelope.event.getClass.getSimpleName + " on tenant " + tenant)

    envelope.event match {
      case p: PlatformEvent => tenantProjection.handlePlatformEvent(p)
      case t: TenantUserEvent => userProjection.handleUserEvent(t)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case event: TenantModified => commitTenantRecords(envelope, event)
      case event: TenantAppliedPlatformUpdate => updateUserIds(event, envelope.offset)
      case _ =>
        logger.warn(s"TenantTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitTenantRecords(envelope: ModelEventEnvelope, tenantModified: TenantModified): Future[Done] = {
    // Tell the projections to prepare for commit, i.e. let them update the persistence.
    tenantProjection.prepareCommit()
    userProjection.prepareCommit()
    // Update the offset of the last event handled in this projection
    persistence.upsert(offsetStorage.createOffsetRecord(envelope.offset))
    // Commit and then inform the last modified registration
    persistence.commit().andThen(_ => {
      // Clear the user cache for those user ids that have been updated
      userProjection.affectedUserIds.foreach(userCache.clear)
      TenantReader.lastModifiedRegistration.handle(tenantModified)
    })
  }

  private def updateUserIds(event: TenantAppliedPlatformUpdate, offset: Offset): Future[Done] = {
    persistence.updateTenantUserInformation(event.tenant, event.newUserInformation.info, offsetStorage.createOffsetRecord(offset))
  }
}
