package org.cafienne.querydb.materializer.consentgroup

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBStorage}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class ConsentGroupEventSink(val caseSystem: CaseSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val system = caseSystem.system

  override val tag: String = ConsentGroupEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(ConsentGroupEventSink.offsetName)

  override def createBatch(persistenceId: String): ConsentGroupEventBatch = new ConsentGroupEventBatch(this, persistenceId, storage)
}

object ConsentGroupEventSink {
  val offsetName = "ConsentGroupEventSink"
}

trait ConsentGroupEventMaterializer {
  val batch: ConsentGroupEventBatch
  lazy val groupId: String = batch.persistenceId
  lazy val dBTransaction: ConsentGroupStorageTransaction = batch.dBTransaction
}
