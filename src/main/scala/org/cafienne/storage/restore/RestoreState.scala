/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.storage.restore

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.storage.actormodel.{ActorMetadata, StorageActorState}
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.archival.Archive
import org.cafienne.storage.restore.event.{ArchiveRetrieved, ChildRestored}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class RestoreState(val actor: ActorDataRestorer) extends StorageActorState with LazyLogging {

  override def handleStorageEvent(event: StorageEvent): Unit = {
    event match {
      case _: ArchiveRetrieved => continueRestoreProcess()
      case _: ChildRestored => continueRestoreProcess()
      case event =>
        logger.error(s"Cannot handle event of type ${event.getClass.getName} in ${actor.getClass.getSimpleName}[${event.actorId}]")
    }
  }

  /** Triggers the archival process upon recovery completion. But only if the ArchivalInitiated event is found.
   */
  def handleRecoveryCompletion(): Unit = {
    printLogMessage(s"Recovery completed with ${events.size} events")
    if (hasArchive) {
      printLogMessage("Continuing recovered archival process")
      continueRestoreProcess()
    }
  }

  override def findCascadingChildren(): Future[Seq[ActorMetadata]] = Future.successful(Seq())

  def hasArchive: Boolean = events.exists(_.isInstanceOf[ArchiveRetrieved])
  lazy val archive: Archive = eventsOfType(classOf[ArchiveRetrieved]).head.archive
  lazy val archives: Seq[Archive] = {
    val list = new ListBuffer[Archive]()
    def addArchive(archive: Archive): Unit = {
      list += archive.copy(children = Seq()) // No need to carry the archives of the children along
      archive.children.foreach(addArchive)
    }
    addArchive(archive)
    list.toSeq
  }

  def continueRestoreProcess(): Unit = {
    val archivesRestored = archives.filter(archive => eventsOfType(classOf[ChildRestored]).exists(_.metadata.actorId == archive.metadata.actorId))
    val archivesPending = archives.filterNot(archive => eventsOfType(classOf[ChildRestored]).exists(_.metadata.actorId == archive.metadata.actorId))
    val archivesToRestore = archivesPending.filter(archive => {
      val parent = archive.metadata.parent
      parent == null || archivesRestored.exists(_.metadata.actorId == parent.actorId)
    })
    if (archivesToRestore.isEmpty) {
      // We're done!!!
      actor.completeRestoreProcess()
    } else {
      printLogMessage(s"Restoring ${archivesToRestore.size} children that have no pending parent")
      archivesToRestore.foreach(actor.initiateChildRestore)
    }
  }
}
