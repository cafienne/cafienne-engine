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

package org.cafienne.service.storage.deletion.state

import org.cafienne.service.storage.actormodel.message.{StorageActionStarted, StorageActionUpdated, StorageEvent}
import org.cafienne.service.storage.actormodel.state.StorageActorState
import org.cafienne.service.storage.deletion.ActorDataRemover
import org.cafienne.service.storage.deletion.event.{QueryDataRemoved, RemovalStarted}

trait DeletionState extends StorageActorState {
  override val actor: ActorDataRemover

  override def handleStorageEvent(event: StorageEvent): Unit = {
    event match {
      case _: StorageActionUpdated =>
        printLogMessage(s"Completed on " + event.getDescription)
        continueStorageProcess()
      case _ => reportUnknownEvent(event)
    }
  }

  var hasStarted: Boolean = false

  override def startStorageProcess(): Unit = {
    if (hasStarted) {
      println(s"$metadata: Starting storage process again, but already reading children and informing owner")
      return
    }
    hasStarted = true
    // No classic event found, using new storage processing
    val children = findCascadingChildren()
    printLogMessage(s"Found ${children.length} children: ${children.mkString("\n--- ", s"\n--- ", "")}")
    informOwner(RemovalStarted(metadata, children))
  }

  override def createStorageStartedEvent: StorageActionStarted = RemovalStarted(metadata, findCascadingChildren())

  /**
   * The removal process is idempotent (i.e., it can be triggered multiple times without ado).
   * It is typically triggered when recovery is done or after the first incoming RemoveActorData command is received.
   * It triggers both child removal and cleaning query data.
   */
  override def continueStorageProcess(): Unit = {
    if (!parentReceivedChildrenInformation) {
      printLogMessage("Triggering storage process, because parent has not replied that children are received")
      startStorageProcess()
    } else {
      if (!timerDataCleared) {
        printLogMessage("Clearing timers")
        clearTimerData()
      } else if (!queryDataCleared) {
        printLogMessage("Deleting query data")
        clearQueryData()
        actor.self ! QueryDataRemoved(metadata)
      }
    }

    checkStorageProcessCompletion()
  }

  /**
   * Determine if all data is removed from children and also from QueryDB.
   * If so, then invoke the final deletion of all actor events, including the StorageEvents that have been created during the deletion process
   */
  override def checkStorageProcessCompletion(): Unit = {
    printLogMessage(s"Running completion check: [queryDataCleared=$queryDataCleared;timerDataCleared=$timerDataCleared;]")
    if (queryDataCleared && timerDataCleared) {
      if (completing) {
        println(s"$metadata: Already completing upon ${events.last.getClass.getSimpleName}")
        return
      }
      completing = true
      actor.completeStorageProcess()
    }
  }

  private var completing: Boolean = false
}
