package org.cafienne.querydb.materializer.consentgroup

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage, OffsetStorageProvider}
import org.cafienne.querydb.materializer.RecordsPersistence
import org.cafienne.querydb.materializer.slick.SlickEventMaterializer

import scala.concurrent.Future

class ConsentGroupEventSink
  (updater: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)
  (implicit val system: ActorSystem, implicit val userCache: IdentityProvider) extends SlickEventMaterializer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val offsetStorage: OffsetStorage = offsetStorageProvider.storage(ConsentGroupEventSink.offsetName)
  override val tag: String = ConsentGroupEvent.TAG

  override def getOffset(): Future[Offset] = offsetStorage.getOffset()

  override def createTransaction(envelope: ModelEventEnvelope): ConsentGroupTransaction = new ConsentGroupTransaction(envelope.persistenceId, updater, userCache, offsetStorage)
}

object ConsentGroupEventSink {
  val offsetName = "ConsentGroupEventSink"
}
