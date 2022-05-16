package org.cafienne.querydb.materializer.cases

import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}

class CaseEventBatch(val sink: CaseEventSink, override val persistenceId: String, val storage: QueryDBStorage) extends QueryDBEventBatch {
  override def createTransaction: CaseTransaction = new CaseTransaction(this, storage)
}
