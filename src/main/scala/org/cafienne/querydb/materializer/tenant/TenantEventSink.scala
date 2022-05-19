package org.cafienne.querydb.materializer.tenant

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.querydb.materializer.{QueryDBEventSink, QueryDBStorage}
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.event.TenantEvent

import scala.concurrent.Future

class TenantEventSink(val caseSystem: CaseSystem, storage: QueryDBStorage) extends QueryDBEventSink with LazyLogging {
  override val tag: String = TenantEvent.TAG

  override def getOffset: Future[Offset] = storage.getOffset(TenantEventSink.offsetName)

  override def createBatch(persistenceId: String): TenantEventBatch = new TenantEventBatch(this, persistenceId, storage)
}

object TenantEventSink {
  val offsetName = "TenantEventSink"
}

trait TenantEventMaterializer {
  val batch: TenantEventBatch
  lazy val tenant: String = batch.persistenceId
  lazy val dBTransaction: TenantStorageTransaction = batch.dBTransaction
}
