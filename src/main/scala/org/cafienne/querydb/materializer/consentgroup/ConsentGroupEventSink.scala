package org.cafienne.querydb.materializer.consentgroup

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.querydb.materializer.slick.{QueryDBEventSink, SlickQueryDBTransaction}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class ConsentGroupEventSink(val caseSystem: CaseSystem) extends QueryDBEventSink with LazyLogging {
  val persistence = new SlickQueryDBTransaction

  lazy val offsetStorage: OffsetStorage = persistence.storage(ConsentGroupEventSink.offsetName)
  override val tag: String = ConsentGroupEvent.TAG

  override def getOffset(): Future[Offset] = offsetStorage.getOffset

  override def createTransaction(envelope: ModelEventEnvelope): ConsentGroupTransaction = new ConsentGroupTransaction(envelope.persistenceId, persistence, caseSystem.userCache)
}

object ConsentGroupEventSink {
  val offsetName = "ConsentGroupEventSink"
}
