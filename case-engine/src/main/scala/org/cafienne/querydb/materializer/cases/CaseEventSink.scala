package org.cafienne.querydb.materializer.cases

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage, OffsetStorageProvider}
import org.cafienne.querydb.materializer.RecordsPersistence
import org.cafienne.querydb.materializer.slick.SlickEventMaterializer

import scala.concurrent.Future

class CaseEventSink
  (persistence: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)
  (implicit override val system: ActorSystem) extends SlickEventMaterializer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val offsetStorage: OffsetStorage = offsetStorageProvider.storage(CaseEventSink.offsetName)
  override val tag: String = CaseEvent.TAG

  override def getOffset(): Future[Offset] = offsetStorage.getOffset()

  override def createTransaction(envelope: ModelEventEnvelope) = new CaseTransaction(envelope.persistenceId, envelope.event.tenant, persistence, offsetStorage)
}

object CaseEventSink {
  val offsetName = "CaseEventSink"
}
