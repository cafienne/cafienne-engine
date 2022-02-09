package org.cafienne.querydb.materializer.tenant

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetRecord}
import org.cafienne.querydb.materializer.RecordsPersistence
import org.cafienne.querydb.materializer.slick.SlickTransaction
import org.cafienne.tenant.actorapi.event._
import org.cafienne.tenant.actorapi.event.deprecated.DeprecatedTenantUserEvent
import org.cafienne.tenant.actorapi.event.platform.PlatformEvent
import org.cafienne.tenant.actorapi.event.user.TenantMemberEvent

import scala.concurrent.Future

class TenantTransaction(tenant: String, persistence: RecordsPersistence, userCache: IdentityProvider) extends SlickTransaction with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val tenantProjection = new TenantProjection(persistence)
  private val userProjection = new TenantUserProjection(persistence)

  def createOffsetRecord(offset: Offset): OffsetRecord = OffsetRecord(TenantEventSink.offsetName, offset)

  override def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    logger.debug("Handling event of type " + envelope.event.getClass.getSimpleName + " on tenant " + tenant)

    envelope.event match {
      case p: PlatformEvent => tenantProjection.handlePlatformEvent(p)
      case m: TenantMemberEvent => userProjection.handleUserEvent(m);
      case t: DeprecatedTenantUserEvent => userProjection.handleDeprecatedUserEvent(t)
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
    persistence.upsert(createOffsetRecord(envelope.offset))
    // Commit and then inform the last modified registration
    persistence.commit().andThen(_ => {
      // Clear the user cache for those user ids that have been updated
      userProjection.affectedUserIds.foreach(userCache.clear)
      TenantReader.lastModifiedRegistration.handle(tenantModified)
    })
  }

  private def updateUserIds(event: TenantAppliedPlatformUpdate, offset: Offset): Future[Done] = {
    persistence.updateTenantUserInformation(event.tenant, event.newUserInformation.info, createOffsetRecord(offset))
  }
}
