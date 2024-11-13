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

package com.casefabric.infrastructure.serialization.serializers

import com.casefabric.infrastructure.serialization.CaseFabricSerializer
import com.casefabric.storage.actormodel.event.ChildrenReceived
import com.casefabric.storage.archival.event._
import com.casefabric.storage.archival.event.cmmn.{CaseArchived, ProcessArchived}
import com.casefabric.storage.archival.response.{ArchivalCompleted, ArchivalRejected}
import com.casefabric.storage.deletion.event._
import com.casefabric.storage.deletion.response.RemovalRejected
import com.casefabric.storage.restore.command.RestoreArchive
import com.casefabric.storage.restore.event._

object StorageSerializers {
  def register(): Unit = {
    registerStorageMessages()
  }

  def registerStorageMessages(): Unit = {
    registerDeletionMessages()
    registerArchivalMessages()
    registerRestoreMessages()
    CaseFabricSerializer.addManifestWrapper(classOf[ChildrenReceived], ChildrenReceived.deserialize)
  }

  def registerDeletionMessages(): Unit = {
    CaseFabricSerializer.addManifestWrapper(classOf[RemovalStarted], RemovalStarted.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[RemovalRequested], RemovalRequested.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[QueryDataRemoved], QueryDataRemoved.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ChildrenRemovalInitiated], ChildrenRemovalInitiated.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[RemovalCompleted], RemovalCompleted.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[RemovalRejected], RemovalRejected.deserialize)
  }

  def registerArchivalMessages(): Unit = {
    // Archival process related events
    CaseFabricSerializer.addManifestWrapper(classOf[ArchivalStarted], ArchivalStarted.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchivalRequested], ArchivalRequested.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[QueryDataArchived], QueryDataArchived.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchiveCreated], ArchiveCreated.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchiveReceived], ArchiveReceived.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchivalCompleted], ArchivalCompleted.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchiveStored], ArchiveStored.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchivalRejected], ArchivalRejected.deserialize)

    // ModelActor related "functional" events (the ones that remain in the journal)
    CaseFabricSerializer.addManifestWrapper(classOf[CaseArchived], CaseArchived.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ProcessArchived], ProcessArchived.deserialize)
  }

  def registerRestoreMessages(): Unit = {
    CaseFabricSerializer.addManifestWrapper(classOf[RestoreRequested], RestoreRequested.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[RestoreArchive], RestoreArchive.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[RestoreStarted], RestoreStarted.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ArchiveRetrieved], ArchiveRetrieved.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[ChildRestored], ChildRestored.deserialize)
    CaseFabricSerializer.addManifestWrapper(classOf[RestoreCompleted], RestoreCompleted.deserialize)
  }
}
