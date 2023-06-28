package org.cafienne.storage.archival

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.{ActorMetadata, OffspringNode}
import org.cafienne.storage.archival.command.ArchiveActorData
import org.cafienne.storage.archival.event.{ArchiveCreated, ArchiveReceived}

class ArchiveNode(val metadata: ActorMetadata, val actor: RootArchiver) extends OffspringNode with LazyLogging {
  override def createStorageCommand: Any = ArchiveActorData(metadata)

  def hasArchive: Boolean = eventsOfType(classOf[ArchiveCreated]).nonEmpty && actor.getChildren(this).forall(_.hasArchive)

  def archive: Archive = {
    if (!hasArchive) {
      val exception = new Exception(s"$this is requesting archive when there is not yet an archive. That's a bug :(")
      logger.warn("Running stacktrace printer on unexpected code path", exception)
      null
    } else {
      val archive = getEvent(classOf[ArchiveCreated]).archive
      // Note: we may want to preserve child ordering
      val childArchives = actor.getChildren(this).map(_.archive)
      archive.copy(children = childArchives)
    }
  }

  override def hasCompleted: Boolean = hasCompletionEvent && actor.getChildren(this).forall(_.hasCompleted)

  override protected def uponReceiveEvent(event: StorageEvent): Unit = event match {
    case event: ArchiveCreated => informActor(new ArchiveReceived(event.metadata))
    case _ => super.uponReceiveEvent(event)
  }
}
