package org.cafienne.querydb.materializer

import akka.Done
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord

import scala.concurrent.Future

trait QueryDBTransaction {

  def upsert(record: OffsetRecord): Unit

  def commit(): Future[Done]
}
