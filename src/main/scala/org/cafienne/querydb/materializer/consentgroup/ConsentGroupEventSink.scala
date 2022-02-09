package org.cafienne.querydb.materializer.consentgroup

import akka.actor.ActorSystem
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.querydb.materializer.slick.{SlickEventMaterializer, SlickRecordsPersistence}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class ConsentGroupEventSink(caseSystem: CaseSystem) extends SlickEventMaterializer with LazyLogging {
  val persistence = new SlickRecordsPersistence

  lazy val offsetStorage: OffsetStorage = persistence.storage(ConsentGroupEventSink.offsetName)
  override val tag: String = ConsentGroupEvent.TAG
  override def system: ActorSystem = caseSystem.system

  override def getOffset(): Future[Offset] = offsetStorage.getOffset

  override def createTransaction(envelope: ModelEventEnvelope): ConsentGroupTransaction = new ConsentGroupTransaction(envelope.persistenceId, persistence, caseSystem.userCache)
}

object ConsentGroupEventSink {
  val offsetName = "ConsentGroupEventSink"
}
