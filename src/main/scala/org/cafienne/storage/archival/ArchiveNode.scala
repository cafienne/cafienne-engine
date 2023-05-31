package org.cafienne.storage.archival

import org.cafienne.storage.actormodel.message.StorageCommand
import org.cafienne.storage.actormodel.{ActorMetadata, OffspringNode}
import org.cafienne.storage.archival.command.ArchiveActorData

class ArchiveNode(val metadata: ActorMetadata, val actor: RootArchiver) extends OffspringNode {
  override def createStorageCommand: StorageCommand = ArchiveActorData(metadata)
}
