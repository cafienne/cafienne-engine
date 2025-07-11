package org.cafienne.service.storage.actormodel.command

import org.cafienne.infrastructure.serialization.JacksonSerializable
import org.cafienne.service.storage.actormodel.ActorMetadata

case class ClearTimerData(metadata: ActorMetadata) extends JacksonSerializable {
}
