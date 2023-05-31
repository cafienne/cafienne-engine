package org.cafienne.storage.restore

import org.cafienne.storage.actormodel.message.StorageCommand
import org.cafienne.storage.actormodel.{ActorMetadata, OffspringNode}
import org.cafienne.storage.restore.command.RestoreActorData

class RestoreNode(val metadata: ActorMetadata, val actor: RootRestorer) extends OffspringNode {
  override def createStorageCommand: StorageCommand = RestoreActorData(metadata)
}
