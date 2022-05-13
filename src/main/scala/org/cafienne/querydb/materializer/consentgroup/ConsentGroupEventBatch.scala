package org.cafienne.querydb.materializer.consentgroup

import org.cafienne.querydb.materializer.QueryDBEventBatch

class ConsentGroupEventBatch(val sink: ConsentGroupEventSink, override val persistenceId: String) extends QueryDBEventBatch {
  override def createTransaction: ConsentGroupTransaction = new ConsentGroupTransaction(this, sink.caseSystem.userCache)
}
