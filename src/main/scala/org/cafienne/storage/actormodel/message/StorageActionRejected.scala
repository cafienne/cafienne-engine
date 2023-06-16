package org.cafienne.storage.actormodel.message

import org.cafienne.infrastructure.serialization.JacksonSerializable

trait StorageActionRejected extends JacksonSerializable {
  val msg: String
}
