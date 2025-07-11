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

package org.cafienne.service.storage.actormodel.state

import org.cafienne.service.storage.actormodel.ActorMetadata
import org.cafienne.service.storage.querydb.ProcessStorage

trait ProcessState extends StorageActorState {
  override val dbStorage: ProcessStorage = new ProcessStorage(actor.caseSystem.queryDB.writer)

  override def findCascadingChildren(): Seq[ActorMetadata] = Seq()

  override def clearQueryData(): Unit = ()// Nothing to delete here, just tell our actor we're done.
}
