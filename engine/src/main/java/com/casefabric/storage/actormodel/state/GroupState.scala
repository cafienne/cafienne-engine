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

package com.casefabric.storage.actormodel.state

import org.apache.pekko.Done
import com.casefabric.storage.actormodel.ActorMetadata
import com.casefabric.storage.querydb.ConsentGroupStorage

import scala.concurrent.Future

trait GroupState extends QueryDBState {
  override val dbStorage: ConsentGroupStorage = new ConsentGroupStorage

  override def findCascadingChildren(): Future[Seq[ActorMetadata]] = Future.successful(Seq())

  override def clearQueryData(): Future[Done] = dbStorage.deleteGroup(metadata.actorId)
}
