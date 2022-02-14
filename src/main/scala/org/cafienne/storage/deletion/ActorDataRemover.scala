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

package org.cafienne.storage.deletion

import akka.actor.{Props, Terminated}
import akka.persistence.DeleteMessagesSuccess
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.storage.actormodel.{ActorMetadata, ActorType, StorageActor}
import org.cafienne.storage.deletion.command.RemoveActorData
import org.cafienne.storage.deletion.event.{ChildrenRemovalInitiated, QueryDataRemoved, RemovalCompleted, RemovalInitiated}
import org.cafienne.storage.deletion.response.RemovalRejected
import org.cafienne.storage.deletion.state._
import org.cafienne.system.CaseSystem

class ActorDataRemover(val caseSystem: CaseSystem, val metadata: ActorMetadata) extends StorageActor[DeletionState] with LazyLogging {
  printLogMessage(s"\n========== Launching Storage Deletion Service ${metadata.path}")

  /**
   * Every type of ModelActor that we operate on has a specific state.
   */
  override def createState(): DeletionState = metadata.actorType match {
    case ActorType.Tenant => new TenantDeletionState(this)
    case ActorType.Case => new CaseDeletionState(this)
    case ActorType.Process => new ProcessDeletionState(this)
    case ActorType.Group => new GroupDeletionState(this)
    case _ => throw new RuntimeException(s"Cannot handle deletion of data on unknown actor type $metadata")
  }

  /**
   * Print a log message to show we're removed from memory.
   */
  override def postStop(): Unit = {
    if (metadata.hasParent) {
      printLogMessage(s"========== Finished Storage Deletion for $metadata (child of ${metadata.parentActorId.path})\n")
    } else {
      printLogMessage(s"========== Finished Storage Deletion for $metadata\n")
    }
  }

  /**
   * Trigger deletion process on the child
   */
  def deleteChildActorData(child: ActorMetadata): Unit = {
    printLogMessage(s"Initiating deletion for child actor $child")

    // First, tell the case system to remove the actual ModelActor (e.g. a Tenant or a Case) from memory
    //  This to avoid continued behavior of that specific actor.
    informCafienneGateway(child.actorId)

    // Now create a child remover and tell it to clean up itself.
    //  Keep watching the child to make sure we know that it is terminated and we need to remove it from the
    //  collection of child references.
    // NOTE: if the child already started the removal process, it will either respond with a RemovalCompleted
    //  or removal initiated. Both is fine, and are handled upon receiveCommand.
    children.getOrElseUpdate(child.actorId, {
      // If the child does not yet exist, create it.
      context.watch(context.actorOf(Props(classOf[ActorDataRemover], caseSystem, child), child.actorId))
    }).tell(command.RemoveActorData(child), self)
  }

  /**
   * System message indicating that a ActorDataRemover has been shutdown by Akka.
   * Typically one of our children that completed ;)
   * Again also tell Cafienne to remove the actual ModelActor from memory
   */
  def childActorTerminated(t: Terminated): Unit = {
    val actorId = t.actor.path.name
    if (children.remove(actorId).isEmpty) {
      logger.warn("Received a Termination message for actor " + actorId + ", but it was not registered in the LocalRoutingService. Termination message is ignored")
    }
    informCafienneGateway(actorId)
  }

  /**
   * When all children and also all QueryDB data is removed, the state object will
   * invoke this method.
   * It simply deletes all events (including StorageEvents!) from the event journal.
   * The akka system will generate a DeleteMessageSuccess, and the receiveCommand method
   * will listen to that and call deletionCompleted() method.
   */
  def completeDeletionProcess(): Unit = {
    printLogMessage(s"Starting final step to delete ourselves from event journal: let akka [delete from journal where persistence_id = '$persistenceId']")
    clearState()
  }

  /**
   * When all our events are also removed from the journal we can tell our parent we're done.
   * Also we'll then remove ourselves from memory.
   */
  def deletionCompleted(msg: String = ""): Unit = {
    if (metadata.hasParent) {
      printLogMessage(s"Completed clearing event journal $msg; informing parent ${metadata.parentActorId} with ref ${context.parent.path} and stopping ActorDataRemover on $metadata")
    } else {
      printLogMessage(s"Completed clearing event journal $msg; informing StorageCoordinator (since we have no parent) and stopping ActorDataRemover on $metadata")
    }
    informCafienneGateway(metadata.actorId)
    context.parent ! RemovalCompleted(metadata)
    context.stop(self)
  }

  /**
   * Validate incoming removal command. Note that the RemoveActorData command is idempotent (can be sent multiple times).
   * Reject the command when:
   * - There are no events found during recovery; this means the persistence id does not exist in event journal
   * - When actor events do not match expected actor type (e.g., trying to delete a "Case" when it actually has TenantEvents)
   * Accept the command when:
   * - Deletion is in progress ==> then return a RemovalInitiated
   * - Deletion is done ==> then return RemovalCompleted.
   *
   * Note: this algorithm makes it possible to e.g. restart the process for children even when they have already
   * completed removal and parent was not yet aware, or when their removal is still in progress.
   * We could extend the "RemovalInitiated" command with additional progress information (e.g., how many children
   * need to be deleted, etc.)
   */
  def validateRemoval(command: RemoveActorData): Unit = {
    if (lastSequenceNr == 0) {
      printLogMessage("Actor has not recovered any events. Probably does not exist at all")
      sender() ! RemovalRejected(command.metadata, "Actor does not exist in the event journal")
    } else if (state.events.nonEmpty && !state.hasExpectedEvents) {
      printLogMessage(s"State does not match expected actor type $metadata; state contains: ${state.actualModelActorType}")
      sender() ! RemovalRejected(command.metadata, s"Expected actor $metadata; Found: ${state.actualModelActorType}")
    } else {
      if (state.events.isEmpty) {
        printLogMessage("Event count is 0")
        deletionCompleted("because there are no events")
      } else {
        storeEvent(RemovalInitiated(command.metadata))
      }
    }
  }

  /**
   * We handle only 1 command, to initiate the process. Rest of incoming traffic is more like events that we need to
   * store to keep track of the deletion state
   */
  override def receiveCommand: Receive = {
    case command: RemoveActorData => validateRemoval(command) // Initial command. Validate and reply.
    case event: ChildrenRemovalInitiated => storeEvent(event) // We now know which children to remove
    case event: RemovalCompleted => storeEvent(event) // One of our children completed
    case event: QueryDataRemoved => storeEvent(event) // Our state is removed from QueryDB
    case _: DeleteMessagesSuccess => deletionCompleted("because events have been deleted from the journal") // Event journal no longer contains our persistence id
    case t: Terminated => childActorTerminated(t) // Akka has removed one of our children from memory
    case _: RemovalInitiated => // Less relevant, unless we use this to retrieve and expose state information
    //      printLogMessage(s"CHILD STARTED! ${childStarted.actorType}[${childStarted.actorId}]")
    case other => printLogMessage(s"Received message with unknown type. Ignoring it. Message is of type ${other.getClass.getName}")
  }
}
