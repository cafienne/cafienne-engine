package org.cafienne.querydb.materializer.tenant

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}
import org.cafienne.tenant.actorapi.event.deprecated.DeprecatedTenantUserEvent
import org.cafienne.tenant.actorapi.event.platform.PlatformEvent
import org.cafienne.tenant.actorapi.event.user.TenantMemberEvent
import org.cafienne.tenant.actorapi.event.{TenantAppliedPlatformUpdate, TenantModified}

import scala.concurrent.Future

class TenantEventBatch(val sink: TenantEventSink, override val persistenceId: String, storage: QueryDBStorage) extends QueryDBEventBatch with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global
  val dBTransaction: TenantStorageTransaction = storage.createTenantTransaction(persistenceId)
  val tenant: String = persistenceId

  private val tenantProjection = new TenantProjection(this)
  private val userProjection = new TenantUserProjection(this)

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
    dBTransaction.upsert(createOffsetRecord(envelope.offset))
    // Commit and then inform the last modified registration
    dBTransaction.commit().andThen(_ => {
      // Clear the user cache for those user ids that have been updated
      userProjection.affectedUserIds.foreach(sink.caseSystem.userCache.clear)
      TenantReader.lastModifiedRegistration.handle(tenantModified)
    })
  }

  private def updateUserIds(event: TenantAppliedPlatformUpdate, offset: Offset): Future[Done] = {
    dBTransaction.updateTenantUserInformation(event.tenant, event.newUserInformation.info, createOffsetRecord(offset))
  }
}
