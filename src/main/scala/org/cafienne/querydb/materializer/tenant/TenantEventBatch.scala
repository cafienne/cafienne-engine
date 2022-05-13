package org.cafienne.querydb.materializer.tenant

import org.cafienne.querydb.materializer.QueryDBEventBatch

class TenantEventBatch(val sink: TenantEventSink, override val persistenceId: String) extends QueryDBEventBatch {
  override def createTransaction: TenantTransaction = new TenantTransaction(this, sink.caseSystem.userCache)
}
