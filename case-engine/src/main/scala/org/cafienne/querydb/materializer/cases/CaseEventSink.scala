package org.cafienne.querydb.materializer.cases

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.querydb.materializer.slick.{SlickEventMaterializer, SlickRecordsPersistence}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class CaseEventSink(caseSystem: CaseSystem) extends SlickEventMaterializer with LazyLogging {
  val persistence = new SlickRecordsPersistence

  override def system: ActorSystem = caseSystem.system

  override val tag: String = CaseEvent.TAG

  override def getOffset(): Future[Offset] = persistence.storage(CaseEventSink.offsetName).getOffset

  override def createTransaction(envelope: ModelEventEnvelope) = new CaseTransaction(envelope.persistenceId, envelope.event.tenant, persistence)
}

object CaseEventSink {
  val offsetName = "CaseEventSink"
}
