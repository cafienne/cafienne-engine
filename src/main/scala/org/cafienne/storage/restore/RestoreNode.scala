package org.cafienne.storage.restore

import org.cafienne.storage.actormodel.{ActorMetadata, OffspringNode}
import org.cafienne.storage.archival.Archive
import org.cafienne.storage.restore.command.RestoreArchive

class RestoreNode(val metadata: ActorMetadata, val actor: RootRestorer) extends OffspringNode {
  override def createStorageCommand: Any = RestoreArchive(metadata, archive)
  var archive: Archive = _

  private def parentCompleted: Boolean = actor.getParent(this).fold(true)(_.hasCompleted)

  override def continueStorageProcess(): Unit = {
    if (parentCompleted) {
      startStorageProcess()
    }
  }
}
