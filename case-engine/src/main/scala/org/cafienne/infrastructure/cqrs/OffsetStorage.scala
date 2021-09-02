package org.cafienne.infrastructure.cqrs

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/**
  * Simple storage for event offset of a certain projection.
  */
trait OffsetStorage extends LazyLogging {
  /**
    * Unique name of the storage.
    */
  val storageName: String
  /**
    * Gets the latest known offset from the storage
    *
    * @return
    */
  def getOffset(): Future[Offset]
  /**
    * Creates a record for the given offset with the storage name. Does not store the record.
    * @param offset
    * @return
    */
  def createOffsetRecord(offset: Offset): OffsetRecord = OffsetRecord(storageName, offset)
}
