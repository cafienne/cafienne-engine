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
import com.casefabric.storage.actormodel.event.{ChildrenReceived, QueryDataCleared}
import com.casefabric.storage.querydb.QueryDBStorage

import scala.concurrent.{ExecutionContext, Future}

trait QueryDBState extends StorageActorState {
  def dbStorage: QueryDBStorage

  implicit def dispatcher: ExecutionContext = dbStorage.dispatcher

  /** Returns true if the RootStorageActor knows about our children
    */
  def parentReceivedChildrenInformation: Boolean = events.exists(_.isInstanceOf[ChildrenReceived])

  /** Returns true if the query database has been cleaned for the ModelActor
    */
  def queryDataCleared: Boolean = events.exists(_.isInstanceOf[QueryDataCleared])

  /** ModelActor specific implementation to clean up the data generated into the QueryDB based on the
    * events of this specific ModelActor.
    */
  def clearQueryData(): Future[Done] = Future.successful(Done)

  /**
    * ModelActor specific implementation. E.g., a Tenant retrieves it's children from the QueryDB,
    * and a Case can determine it based on the PlanItemCreated events it has.
    *
    * @return
    */
  def findCascadingChildren(): Future[Seq[ActorMetadata]] = Future.successful(Seq())
}
