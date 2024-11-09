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
