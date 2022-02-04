package org.cafienne.service.db.materializer.tenant

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage, OffsetStorageProvider}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.slick.SlickEventMaterializer
import org.cafienne.tenant.actorapi.event.TenantEvent

import scala.concurrent.Future

class TenantEventSink
  (updater: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)
  (implicit val system: ActorSystem, implicit val userCache: IdentityProvider) extends SlickEventMaterializer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val offsetStorage: OffsetStorage = offsetStorageProvider.storage(TenantEventSink.offsetName)
  override val tag: String = TenantEvent.TAG

  override def getOffset(): Future[Offset] = offsetStorage.getOffset()

  override def createTransaction(envelope: ModelEventEnvelope): TenantTransaction = new TenantTransaction(envelope.persistenceId, updater, userCache, offsetStorage)
}

object TenantEventSink {
  val offsetName = "TenantEventSink"
}
