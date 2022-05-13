package org.cafienne.querydb.materializer.cases

import org.cafienne.querydb.materializer.QueryDBEventBatch

class CaseEventBatch(val sink: CaseEventSink, override val persistenceId: String) extends QueryDBEventBatch {
  override def createTransaction: CaseTransaction = new CaseTransaction(this)
}
