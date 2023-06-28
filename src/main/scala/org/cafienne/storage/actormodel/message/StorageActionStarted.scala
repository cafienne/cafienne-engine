package org.cafienne.storage.actormodel.message

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.storage.actormodel.ActorMetadata

trait StorageActionStarted extends StorageEvent {
  val children: Seq[ActorMetadata]

  override def write(generator: JsonGenerator): Unit = {
    super.writeStorageEvent(generator)
    writeField(generator, Fields.children, children.map(_.toString))
  }
}
