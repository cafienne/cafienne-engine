package org.cafienne.querydb.materializer.consentgroup

import org.cafienne.querydb.materializer.{QueryDBEventBatch, QueryDBStorage}

class ConsentGroupEventBatch(val sink: ConsentGroupEventSink, override val persistenceId: String, storage: QueryDBStorage) extends QueryDBEventBatch {
  override def createTransaction: ConsentGroupTransaction = new ConsentGroupTransaction(this, sink.caseSystem.userCache, storage)
}
