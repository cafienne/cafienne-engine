package org.cafienne.service.api.writer

import akka.persistence.query.Offset
import org.cafienne.infrastructure.cqrs.{OffsetStorage, OffsetStorageProvider}

import scala.concurrent.Future

/**
  * Simple test offset storage. Always returns Offset.noOffset
  */
object NoOffsetStorage extends OffsetStorageProvider() {
  override def storage(storageName: String): OffsetStorage = {
    new OffsetStorage {
      override val name: String = storageName
      override def getOffset(): Future[Offset] = Future.successful(Offset.noOffset)
    }
  }
}
