package org.cafienne.service.db.materializer.tenant

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.cqrs.{OffsetStorage, OffsetStorageProvider}
import org.cafienne.service.api.tenant.TenantReader
import org.cafienne.service.db.materializer.slick.SlickEventMaterializer
import org.cafienne.service.db.materializer.{LastModifiedRegistration, RecordsPersistence}
import org.cafienne.service.db.query.UserQueries
import org.cafienne.tenant.actorapi.event.TenantEvent

import scala.concurrent.Future

class TenantProjectionsWriter
  (userQueries: UserQueries, updater: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)
  (implicit val system: ActorSystem, implicit val userCache: IdentityProvider) extends SlickEventMaterializer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val offsetStorage: OffsetStorage = offsetStorageProvider.storage("TenantProjectionsWriter")
  override val tag: String = TenantEvent.TAG
  override val lastModifiedRegistration: LastModifiedRegistration = TenantReader.lastModifiedRegistration

  override def getOffset(): Future[Offset] = offsetStorage.getOffset()

  override def createTransaction(actorId: String, tenant: String): TenantTransaction = new TenantTransaction(actorId, userQueries, updater, userCache, offsetStorage)
}
