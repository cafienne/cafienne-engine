package org.cafienne.querydb.materializer.cases

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.querydb.materializer.QueryDBOffsetStore
import org.cafienne.querydb.materializer.slick.QueryDBEventSink
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class CaseEventSink(val caseSystem: CaseSystem) extends QueryDBEventSink with LazyLogging {
  override val tag: String = CaseEvent.TAG

  override def getOffset: Future[Offset] = QueryDBOffsetStore(CaseEventSink.offsetName).getOffset

  override def createTransaction(envelope: ModelEventEnvelope) = new CaseTransaction(envelope.persistenceId, envelope.event.tenant)
}

object CaseEventSink {
  val offsetName = "CaseEventSink"
}
