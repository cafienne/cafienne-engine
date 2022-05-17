package org.cafienne.querydb.materializer.cases

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBStorage}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class CaseEventSink(val caseSystem: CaseSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val tag: String = CaseEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(CaseEventSink.offsetName)

  override def createBatch(persistenceId: String): CaseEventBatch = new CaseEventBatch(this, persistenceId, storage)
}

object CaseEventSink {
  val offsetName = "CaseEventSink"
}

trait CaseEventMaterializer {
  val batch: CaseEventBatch
  lazy val caseInstanceId: String = batch.persistenceId
  lazy val dBTransaction: CaseStorageTransaction = batch.dBTransaction
  lazy val tenant: String = batch.tenant
}
