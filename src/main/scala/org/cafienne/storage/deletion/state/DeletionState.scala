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

import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.state.StorageActorState
import org.cafienne.storage.deletion.ActorDataRemover
import org.cafienne.storage.deletion.event.{ChildrenRemovalInitiated, QueryDataRemoved, RemovalCompleted, RemovalInitiated}

trait DeletionState extends StorageActorState {
  override val actor: ActorDataRemover

  override def handleStorageEvent(event: StorageEvent): Unit = event match {
    case event: RemovalInitiated =>
      printLogMessage(s"Starting removal for ${event.metadata}")
      continueStorageProcess()
    case event: ChildrenRemovalInitiated =>
      printLogMessage(s"Initiating deletion of ${event.members.size} children")
      triggerChildRemovalProcess()
    case event: QueryDataRemoved =>
      printLogMessage(s"QueryDB has been cleaned")
      checkDeletionProcessCompletion()
    case event: RemovalCompleted =>
      printLogMessage(s"Child ${event.metadata} reported completion")
      checkDeletionProcessCompletion()
    case event =>
      printLogMessage(
        s"Encountered unexpected storage event ${event.getClass.getName} on Actor [${event.actorId}] data on behalf of user ${event.user}"
      )
  }

  /** The removal process is idempotent (i.e., it can be triggered multiple times without ado).
    * It is typically triggered when recovery is done or after the first incoming RemoveActorData command is received.
    * It triggers both child removal and cleaning query data.
    */
  override def continueStorageProcess(): Unit = {
    triggerChildRemovalProcess()
    triggerQueryDBCleanupProcess()
  }

  /** Child removal process consists of 2 steps:
    * 1. Determine what the children are, this is done by the ModelActor specific state (e.g., a Tenant needs QueryDB info,
    * cases can do it with PlanItemCreated events).
    * When the info is found, it is sent to self as a command, such that we can store the event from it
    * 2. When the event with the children metadata is available, we can trigger each of those children
    * to delete themselves. When the child is fully cleaned (including it's own children), it reports back to us.
    */
  def triggerChildRemovalProcess(): Unit = {
    if (!childrenMetadataAvailable) {
      // Use the db storage connection pool to provide threads.
      //  The reason is that only the query db relevant futures will need to do an actual logic
      findCascadingChildren().map { children =>
        printLogMessage(s"Found ${children.length} children to be removed: ${children.mkString("\n--- ", s"\n--- ", "")}")
        actor.self ! ChildrenRemovalInitiated(actor.metadata, children)
      }
    } else {
      if (pendingChildRemovals.nonEmpty) {
        printLogMessage(s"Found ${pendingChildRemovals.size} out of ${children.size} children with pending removal")
        pendingChildRemovals.foreach(actor.deleteChildActorData)
      } else {
        printLogMessage(s"No children found that have pending removal")
      }
      checkDeletionProcessCompletion()
    }
  }

  /** Let the ModelActor specific state clean up the QueryDB unless it is already done
    */
  def triggerQueryDBCleanupProcess(): Unit = {
    if (!queryDataCleared) {
      printLogMessage("Deleting query data")
      clearQueryData().map(_ => actor.self ! QueryDataRemoved(metadata))
    }
    checkDeletionProcessCompletion()
  }

  /** Determine if we have an event with the metadata of all our children
    */
  def childrenMetadataAvailable: Boolean = events.exists(_.isInstanceOf[ChildrenRemovalInitiated])

  /** Retrieve metadata of all our children. Only makes sense if the children metadata is available...
    */
  def children: Seq[ActorMetadata] =
    events.filter(_.isInstanceOf[ChildrenRemovalInitiated]).map(_.asInstanceOf[ChildrenRemovalInitiated]).flatMap(_.members).toSeq

  /** Determine which children have not yet reported back that their data is removed
    */
  def pendingChildRemovals: Seq[ActorMetadata] = children.filterNot(isAlreadyDeleted)

  /** Returns true if there is a ChildRemovalCompleted event for this child metadata
    */
  def isAlreadyDeleted(child: ActorMetadata): Boolean =
    events.filter(_.isInstanceOf[RemovalCompleted]).map(_.asInstanceOf[RemovalCompleted]).exists(_.metadata == child)

  /** Returns true if the query database has been cleaned for the ModelActor
    */
  def queryDataCleared: Boolean = events.exists(_.isInstanceOf[QueryDataRemoved])

  /** Determine if all data is removed from children and also from QueryDB.
    * If so, then invoke the final deletion of all actor events, including the StorageEvents that have been created during the deletion process
    */
  def checkDeletionProcessCompletion(): Unit = {
    printLogMessage(s"Running completion check: [queryDataCleared=$queryDataCleared; childrenMetadataAvailable=$childrenMetadataAvailable; children removed=${children.size - pendingChildRemovals.size}, pending=${pendingChildRemovals.size}]")
    if (childrenMetadataAvailable && pendingChildRemovals.isEmpty && queryDataCleared) {
      actor.completeDeletionProcess()
    }
  }
}
