package org.cafienne.storage.actormodel.command

import org.cafienne.infrastructure.serialization.JacksonSerializable
import org.cafienne.storage.actormodel.ActorMetadata

case class ClearTimerData(metadata: ActorMetadata) extends JacksonSerializable {
}
