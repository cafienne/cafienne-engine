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

package com.casefabric.storage.restore

import com.casefabric.storage.actormodel.{ActorMetadata, OffspringNode}
import com.casefabric.storage.archival.Archive
import com.casefabric.storage.restore.command.RestoreArchive

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
