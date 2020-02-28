package org.cafienne.service.api.projection.participants

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.cqrs.TaggedEventConsumer
import org.cafienne.infrastructure.jdbc.OffsetStorage
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tenant.UserQueries
import org.cafienne.tenant.akka.event.{TenantEvent, TenantModified}

import scala.concurrent.Future

class TenantProjectionsWriter
  (userQueries: UserQueries, updater: RecordsPersistence, override val offsetStorage: OffsetStorage)
  (implicit val system: ActorSystem, implicit val userCache: IdentityProvider) extends LazyLogging with OffsetStorage with TaggedEventConsumer {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val offsetStorageName = "TenantProjectionsWriter"
  override val eventTag: String = TenantEvent.TAG

  private val transactionCache = new scala.collection.mutable.HashMap[String, TenantTransaction]
  private def getTransaction(tenantId: String) = transactionCache.getOrElseUpdate(tenantId, new TenantTransaction(tenantId, userQueries, updater))

  def consumeModelEvent(newOffset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]): Future[Done] = {
    modelEvent match {
      case evt: TenantEvent => {
        val tenant = evt.getActorId
        val transaction = getTransaction(tenant)
        transaction.handleEvent(evt).flatMap(_ => {
          evt match {
            case _: TenantModified => {
              // Remove transaction with tenant records and commit it
              //  Also update the TenantReader if we add one.
              transactionCache.remove(tenant)
              transaction.commit(offsetStorageName, newOffset).flatMap(_ => {
                userCache.clear(transaction.modifiedUsers)
                // TenantReader.inform(tm)
                Future.successful(Done)
              })
            }
            case _ => Future.successful(Done)
          }
        })
      }
      case other => {
        logger.error("Ignoring unexpected model event of type '" + other.getClass.getName() + ". Event has offset: " + newOffset + ", persistenceId: " + persistenceId + ", sequenceNumber: " + sequenceNr)
        Future.successful(Done)
      }
    }
  }
}
