package org.cafienne.storage.deletion

import org.cafienne.storage.actormodel.{ActorMetadata, OffspringNode}
import org.cafienne.storage.deletion.command.RemoveActorData

class RemoveNode(val metadata: ActorMetadata, val actor: RootRemover) extends OffspringNode {
  override def createStorageCommand: Any = RemoveActorData(metadata)
}
