package org.cafienne.querydb.materializer.tenant

import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}

class TenantEventBatch(val sink: TenantEventSink, override val persistenceId: String, storage: QueryDBStorage) extends QueryDBEventBatch {
  override def createTransaction: TenantTransaction = new TenantTransaction(this, sink.caseSystem.userCache, storage)
}
