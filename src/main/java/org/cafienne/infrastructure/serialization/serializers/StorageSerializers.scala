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

package org.cafienne.infrastructure.serialization.serializers

import org.cafienne.infrastructure.serialization.CafienneSerializer
import org.cafienne.storage.archival.event._
import org.cafienne.storage.deletion.event.{ChildrenRemovalInitiated, QueryDataRemoved, RemovalCompleted, RemovalInitiated}
import org.cafienne.storage.restore.command.RestoreArchive
import org.cafienne.storage.restore.event.{ArchiveRetrieved, ChildRestored, RestoreInitiated}

object StorageSerializers {
  def register(): Unit = {
    registerStorageMessages()
  }

  def registerStorageMessages(): Unit = {
    registerDeletionMessages()
    registerArchivalMessages()
    registerRestoreMessages()
  }

  def registerDeletionMessages(): Unit = {
    CafienneSerializer.addManifestWrapper(classOf[RemovalInitiated], RemovalInitiated.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[QueryDataRemoved], QueryDataRemoved.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ChildrenRemovalInitiated], ChildrenRemovalInitiated.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[RemovalCompleted], RemovalCompleted.deserialize)
  }

  def registerArchivalMessages(): Unit = {
    // Archival process related events
    CafienneSerializer.addManifestWrapper(classOf[ArchivalInitiated], ArchivalInitiated.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[QueryDataArchived], QueryDataArchived.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ChildrenArchivalInitiated], ChildrenArchivalInitiated.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ChildArchived], ChildArchived.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ArchiveCreated], ArchiveCreated.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ArchiveExported], ArchiveExported.deserialize)

    // ModelActor related "functional" events (the ones that remain in the journal)
    CafienneSerializer.addManifestWrapper(classOf[CaseArchived], CaseArchived.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ProcessArchived], ProcessArchived.deserialize)
  }

  def registerRestoreMessages(): Unit = {
    CafienneSerializer.addManifestWrapper(classOf[RestoreArchive], RestoreArchive.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[RestoreInitiated], RestoreInitiated.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ArchiveRetrieved], ArchiveRetrieved.deserialize)
    CafienneSerializer.addManifestWrapper(classOf[ChildRestored], ChildRestored.deserialize)
  }
}
