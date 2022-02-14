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

package org.cafienne.storage.archival.state

import akka.Done
import org.cafienne.storage.actormodel.CaseChildrenFinder
import org.cafienne.storage.archival.ActorDataArchiver
import org.cafienne.storage.archival.event.{CaseArchived, ModelActorArchived}
import org.cafienne.storage.querydb.CaseStorage

import scala.concurrent.Future

class CaseArchivalState(override val actor: ActorDataArchiver) extends ArchivalState with CaseChildrenFinder {
  override val dbStorage: CaseStorage = new CaseStorage

  override def archiveQueryData(): Future[Done] = {
    dbStorage.archiveCase(metadata.actorId)
  }

  override def createArchivedEvent: ModelActorArchived = new CaseArchived(metadata)
}
