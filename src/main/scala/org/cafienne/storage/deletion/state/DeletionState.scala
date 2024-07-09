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

package org.cafienne.storage.deletion.state

import org.apache.pekko.Done
import org.cafienne.storage.actormodel.event.{ChildrenReceived, QueryDataCleared}
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.state.QueryDBState
import org.cafienne.storage.deletion.ActorDataRemover
import org.cafienne.storage.deletion.event.{QueryDataRemoved, RemovalStarted}

import scala.concurrent.Future

trait DeletionState extends QueryDBState {
  override val actor: ActorDataRemover

  override def handleStorageEvent(event: StorageEvent): Unit = {
    event match {
      case _: ChildrenReceived =>
        printLogMessage(s"Stored children information")
        continueStorageProcess()
      case _: QueryDataCleared =>
        printLogMessage(s"QueryDB has been cleaned")
        continueStorageProcess()
      case _ => reportUnknownEvent(event)
    }
  }

  /** ModelActor specific implementation to clean up the data generated into the QueryDB based on the
    * events of this specific ModelActor.
    */
  def clearQueryData(): Future[Done]

  var hasStarted: Boolean = false

  override def startStorageProcess(): Unit = {
    if (hasStarted) {
      println(s"$metadata: Starting storage process again, but already reading children and informing owner")
      return
    }
    hasStarted = true
    // No classic event found, using new storage processing
    findCascadingChildren().map { children =>
      printLogMessage(s"Found ${children.length} children: ${children.mkString("\n--- ", s"\n--- ", "")}")
      informOwner(RemovalStarted(metadata, children))
    }
  }

  /** The removal process is idempotent (i.e., it can be triggered multiple times without ado).
    * It is typically triggered when recovery is done or after the first incoming RemoveActorData command is received.
    * It triggers both child removal and cleaning query data.
    */
  override def continueStorageProcess(): Unit = {
    if (!parentReceivedChildrenInformation) {
      printLogMessage("Triggering storage process, because parent has not replied that children are received")
      startStorageProcess()
    } else {
      if (!queryDataCleared) {
        printLogMessage("Deleting query data")
        clearQueryData().map(_ => actor.self ! QueryDataRemoved(metadata))
      } else { // Children found and query data cleared
        if (completing) {
          println(s"$metadata: Already completing upon ${events.last.getClass.getSimpleName}")
          return
        }
        completing = true
        actor.completeStorageProcess()
      }
    }
  }

  private var completing: Boolean = false
}
