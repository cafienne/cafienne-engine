package org.cafienne.infrastructure.cqrs.javadsl

import akka.persistence.query.Offset
import scala.concurrent.Future

/**
  * Java wrapper
  *
  * @param name
  * @param getOffset
  */
class OffsetStorage(name: String, getOffset: (() => Future[Offset])) extends org.cafienne.infrastructure.cqrs.offset.OffsetStorage {
  /**
    * Unique name of the storage.
    */
  override val storageName: String = name

  /**
    * Gets the latest known offset from the storage
    *
    * @return
    */
  override def getOffset: Future[Offset] = getOffset()
}
