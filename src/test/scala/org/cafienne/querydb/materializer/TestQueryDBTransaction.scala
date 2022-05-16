package org.cafienne.querydb.materializer

import akka.Done
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class TestQueryDBTransaction(val persistenceId: String) extends QueryDBTransaction {

  println("\n\nCreating test query db transaction")

  val records: ListBuffer[AnyRef] = ListBuffer()

  def addRecord(record: AnyRef): Unit = {
    records += record
    println(s"Added record of type ${record.getClass.getSimpleName}, now having ${records.size} records")
  }

  override def upsert(record: OffsetRecord): Unit = addRecord(record)

  override def commit(): Future[Done] = Future.successful(Done)
}
