package org.cafienne.storage.archival

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.archival.event.ArchiveStored
import org.cafienne.storage.archive.Storage

import scala.concurrent.ExecutionContext

class RootArchiveNode(metadata: ActorMetadata, actor: RootArchiver) extends ArchiveNode(metadata, actor) with LazyLogging {
  override def hasCompleted: Boolean =
    // If we're root, we're also awaiting confirmation of actual storage of the archive
    eventsOfType(classOf[ArchiveStored]).nonEmpty

  private var startedExporting = false

  override def continueStorageProcess(): Unit = {
    if (hasArchive) {
      if (!startedExporting) { //  Use the system dispatcher for handling the export success
        implicit val ec: ExecutionContext = actor.caseSystem.system.dispatcher

        val storage: Storage = Cafienne.config.engine.storage.archive.plugin
        storage.store(archive).map(_ => actor.self ! ArchiveStored(metadata))
        startedExporting = true
      }
    } else {
      super.continueStorageProcess()
    }
  }
}
