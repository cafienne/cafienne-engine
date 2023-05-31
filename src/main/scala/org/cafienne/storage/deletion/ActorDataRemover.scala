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
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.{ActorMetadata, ActorType, QueryDBStorageActor}
import org.cafienne.storage.deletion.command.RemoveActorData
import org.cafienne.storage.deletion.event.{RemovalCompleted, RemovalStarted}
import org.cafienne.storage.deletion.response.RemovalRejected
import org.cafienne.storage.deletion.state._
import org.cafienne.system.CaseSystem

class ActorDataRemover(val caseSystem: CaseSystem, val metadata: ActorMetadata) extends QueryDBStorageActor[DeletionState] with LazyLogging {
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
      printLogMessage(s"========== Finished Storage Deletion for $metadata (child of ${metadata.parent.path})\n")
    } else {
      printLogMessage(s"========== Finished Storage Deletion for $metadata\n")
    }
  }

  /**
   * Trigger deletion process on the child
   */
  def deleteChildActorData(child: ActorMetadata): Unit = {
    printLogMessage(s"Initiating deletion for child actor $child")

    if (child.actorId == null) {
      println(s"$metadata: Cannot delete child actor data for empty child actor?!")
      return
    }

    // First, tell the case system to remove the actual ModelActor (e.g. a Tenant or a Case) from memory
    //  This to avoid continued behavior of that specific actor.
    terminateModelActor(child, {
      // Now create a child remover and tell it to clean up itself.
      //  Keep watching the child to make sure we know that it is terminated and we need to remove it from the
      //  collection of child references.
      // NOTE: if the child already started the removal process, it will either respond with a RemovalCompleted
      //  or removal initiated. Both is fine, and are handled upon receiveCommand.
      getActorRef(child, Props(classOf[ActorDataRemover], caseSystem, child)).tell(command.RemoveActorData(child), self)
    })
  }

  /**
   * When all children and also all QueryDB data is removed, the state object will
   * invoke this method.
   * It simply deletes all events (including StorageEvents!) from the event journal.
   * The akka system will generate a DeleteMessageSuccess, and the receiveCommand method
   * will listen to that and call deletionCompleted() method.
   */
  def completeStorageProcess(): Unit = {
    printLogMessage(s"Starting final step to delete ourselves from event journal: let akka [delete from journal where persistence_id = '$persistenceId']")
    clearState()
  }

  /**
   * When all our events are also removed from the journal we can tell our parent we're done.
   * Also we'll then remove ourselves from memory.
   */
  def afterStorageProcessCompleted(msg: String = ""): Unit = {
    if (metadata.hasParent) {
      printLogMessage(s"Completed clearing event journal $msg; informing parent ${metadata.parent} with ref ${context.parent.path} and stopping ActorDataRemover on $metadata")
    } else {
      printLogMessage(s"Completed clearing event journal $msg; informing StorageCoordinator (since we have no parent) and stopping ActorDataRemover on $metadata")
    }
    terminateModelActor(metadata)
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
  def startStorageProcess(command: RemoveActorData): Unit = {
    if (lastSequenceNr == 0) {
      printLogMessage("Actor has not recovered any events. Probably does not exist at all")
      sender() ! RemovalRejected(command.metadata, "Actor does not exist in the event journal")
    } else if (state.events.nonEmpty && !state.hasExpectedEvents) {
      printLogMessage(s"State does not match expected actor type $metadata; state contains: ${state.actualModelActorType}")
      sender() ! RemovalRejected(command.metadata, s"Expected actor $metadata; Found: ${state.actualModelActorType}")
    } else {
      if (state.events.isEmpty) {
        printLogMessage("Event count is 0")
        afterStorageProcessCompleted("because there are no events")
      } else {
        startStorageProcess()
      }
    }
  }

  /**
   * We handle only 1 command, to initiate the process. Rest of incoming traffic is more like events that we need to
   * store to keep track of the deletion state
   */
  override def receiveCommand: Receive = {
    case command: RemoveActorData => startStorageProcess(command) // Initial command. Validate and reply.
    case event: StorageEvent => storeEvent(event) // We now know which children to remove
    case _: DeleteMessagesSuccess => afterStorageProcessCompleted("because events have been deleted from the journal") // Event journal no longer contains our persistence id
    case t: Terminated => removeActorRef(t) // Akka has removed one of our children from memory
    case other => reportUnknownMessage(other)
  }
}
