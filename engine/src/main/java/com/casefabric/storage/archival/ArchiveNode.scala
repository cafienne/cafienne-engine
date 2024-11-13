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

package com.casefabric.storage.archival

import com.typesafe.scalalogging.LazyLogging
import com.casefabric.storage.actormodel.message.StorageEvent
import com.casefabric.storage.actormodel.{ActorMetadata, OffspringNode}
import com.casefabric.storage.archival.command.ArchiveActorData
import com.casefabric.storage.archival.event.{ArchiveCreated, ArchiveReceived}

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
