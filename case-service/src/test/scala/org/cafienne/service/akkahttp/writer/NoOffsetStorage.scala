package org.cafienne.service.akkahttp.writer

import akka.persistence.query.Offset
import org.cafienne.infrastructure.cqrs.{OffsetStorage, OffsetStorageProvider}

import scala.concurrent.Future

/**
  * Simple test offset storage. Always returns Offset.noOffset
  */
object NoOffsetStorage extends OffsetStorageProvider() {
  override def storage(name: String): OffsetStorage = {
    new OffsetStorage {
      override val storageName: String = name
      override def getOffset(): Future[Offset] = Future.successful(Offset.noOffset)
    }
  }
}
