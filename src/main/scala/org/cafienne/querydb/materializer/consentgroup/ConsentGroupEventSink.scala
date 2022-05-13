package org.cafienne.querydb.materializer.consentgroup

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.infrastructure.cqrs.batch.EventBatch
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBOffsetStore}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class ConsentGroupEventSink(val caseSystem: CaseSystem) extends QueryDBEventSink with LazyLogging {
  override val tag: String = ConsentGroupEvent.TAG

  override def getOffset: Future[Offset] = QueryDBOffsetStore(ConsentGroupEventSink.offsetName).getOffset

  override def createTransaction(batch: EventBatch): ConsentGroupTransaction = new ConsentGroupTransaction(batch, caseSystem.userCache)
}

object ConsentGroupEventSink {
  val offsetName = "ConsentGroupEventSink"
}
