package org.cafienne.storage.actormodel.message

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.Fields

trait StorageActionRejected extends StorageActionCompleted {
  val msg: String

  override def write(generator: JsonGenerator): Unit = {
    super.writeStorageEvent(generator)
    writeField(generator, Fields.message, msg)
  }
}
