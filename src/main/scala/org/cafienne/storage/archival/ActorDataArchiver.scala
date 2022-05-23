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

package org.cafienne.storage.archival

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.DeleteMessagesSuccess
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.storage.actormodel.{ActorMetadata, ActorType, StorageActor}
import org.cafienne.storage.archival.command.ArchiveActorData
import org.cafienne.storage.archival.event._
import org.cafienne.storage.archival.response.ArchivalRejected
import org.cafienne.storage.archival.state.{ArchivalState, CaseArchivalState, ProcessArchivalState}
import org.cafienne.storage.archive.Storage
import org.cafienne.system.CaseSystem

import scala.concurrent.ExecutionContext

class ActorDataArchiver(override val caseSystem: CaseSystem, override val metadata: ActorMetadata) extends StorageActor[ArchivalState] with LazyLogging {

  printLogMessage(s"\n========== Launching Storage Archival Service ${metadata.path}")

  override def createState(): ArchivalState = metadata.actorType match {
    case ActorType.Case => new CaseArchivalState(this)
    case ActorType.Process => new ProcessArchivalState(this)
//    case ActorType.Tenant => new TenantDeletionState(this)
//    case ActorType.Group => new GroupDeletionState(this)
    case _ => throw new RuntimeException(s"Cannot handle archival of data on actor type $metadata")
  }

  /**
   * Print a log message to show we're removed from memory.
   */
  override def postStop(): Unit = {
    if (metadata.hasParent) {
      printLogMessage(s"========== Finished Storage Archival for $metadata (child of ${metadata.parentActorId.path})\n")
    } else {
      printLogMessage(s"========== Finished Storage Archival for $metadata\n")
    }
  }

  /**
   * Trigger deletion process on the child
   */
  def archiveChildActorData(child: ActorMetadata): Unit = {
    printLogMessage(s"Initiating deletion for child actor $child")

    // First, tell the case system to remove the actual ModelActor (e.g. a Tenant or a Case) from memory
    //  This to avoid continued behavior of that specific actor.
    informCafienneGateway(child.actorId)

    // Now create a child archiver and tell it to clean up itself.
    //  Keep watching the child to make sure we know that it is terminated and we need to remove it from the
    //  collection of child references.
    // NOTE: if the child already started the archival process, it will either respond with a ArchivalCompleted
    //  or archival initiated. Both is fine, and are handled upon receiveCommand.
    getChildActorRef(child).tell(ArchiveActorData(child), self)
  }

  def getChildActorRef(child: ActorMetadata): ActorRef = {
    children.getOrElseUpdate(child.actorId, {
      // If the child does not yet exist, create it.
      context.watch(context.actorOf(Props(classOf[ActorDataArchiver], caseSystem, child), child.actorId))
    })
  }

  /**
   * System message indicating that a ActorDataArchiver has been shutdown by Akka.
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

  def archivalCompleted(): Unit = {
    context.stop(self)
  }

  /**
   * Invoked when e.g. CaseArchived or ProcessArchived is stored
   */
  def afterModelActorEventStored(): Unit = {
    // Archival completed; make sure "CaseArchived", etc. exist, and if so, delete the other messages.
    if (state.events.exists(_.isInstanceOf[ModelActorArchived])) {
      clearState(lastSequenceNr - 1)
    }
  }

  def afterArchiveExported(): Unit = {
    printLogMessage("Found acknowledgement that our archive has been exported")
    storeEvent(state.createModelActorEvent)
  }

  def afterArchiveCreated(event: ArchiveCreated): Unit = {
    // When the Archive is available, we have two options, depending on whether we are the root or a child.
    // 1. If we are the root, then we need to bring the archive to storage, and after that add ArchiveExported.
    // 2. If we are child, then we send our archive to our parent, and the parent will send us ArchiveExported as
    //    an acknowledgement.
    // If both cases, ultimately, ArchiveExported is persisted, which triggers the rest of our archival process.
    if (metadata.isRoot) {
      //  Use the system dispatcher for handling the export success
      implicit val ec: ExecutionContext = caseSystem.system.dispatcher

      val storage: Storage = Cafienne.config.storage.archive
      storage.store(event.archive).map(_ => self ! ArchiveExported(event.metadata))
    } else {
      // Inform parent about our archive. Parent will send us "ArchiveExported"
      context.parent ! ChildArchived(metadata = metadata, archive = event.archive)
    }
  }

  /**
   * When all children and also all QueryDB data is archived, the state object will
   * invoke this method.
   * This will create an Archive and persist it as event,
   * then, the handling of that event continues the process
   */
  def createArchive(): Unit = {
    storeEvent(state.createArchiveEvent)
  }

  /**
   * Validate incoming archival command. Note that the ArchiveActorData command is idempotent (can be sent multiple times).
   * Reject the command when:
   * - There are no events found during recovery; this means the persistence id does not exist in event journal
   * - When actor events do not match expected actor type (e.g., trying to delete a "Case" when it actually has TenantEvents)
   * Accept the command when:
   * - Archival is in progress ==> then store ArchivalInitiated
   * - Archival is done ==> then return ArchivalCompleted.
   *
   */
  def validateArchival(command: ArchiveActorData): Unit = {
    if (lastSequenceNr == 0) {
      printLogMessage("Actor has not recovered any events. Probably does not exist at all")
      sender() ! ArchivalRejected(command.metadata, "Actor does not exist in the event journal")
    } else if (state.events.nonEmpty && !state.hasExpectedEvents) {
      printLogMessage(s"State does not match expected actor type $metadata; state contains: ${state.actualModelActorType}")
      sender() ! ArchivalRejected(command.metadata, s"Expected actor $metadata; Found: ${state.actualModelActorType}")
    } else {
      if (state.isCleared) {
        // No need to do anything, as our parent is informed and we can simply go offline again
        afterModelActorEventStored()
      } else if (state.isExported) {
        printLogMessage("Our parent is aware that we are archived, but we have not yet cleaned up ourself, doing that now")
        afterArchiveExported()
      } else if (state.isCreated) {
        afterArchiveCreated(state.archive)
      } else {
        storeEvent(ArchivalInitiated(command.metadata))
      }
    }
  }

  def confirmChildArchived(event: ChildArchived): Unit = {
    val child = event.metadata
    printLogMessage(s"Received archive for child $child; sending acknowledgement")
    getChildActorRef(child).tell(ArchiveExported(child), self)
  }

  /**
   * We handle only 1 command, to initiate the process. Rest of incoming traffic is more like events that we need to
   * store to keep track of the deletion state
   */
  override def receiveCommand: Receive = {
    case command: ArchiveActorData => validateArchival(command) // Initial command. Validate and reply.
    case event: ChildrenArchivalInitiated => storeEvent(event) // We now know which children to archive
    case event: QueryDataArchived => storeEvent(event) // Our state is archived from QueryDB
    case event: ChildArchived => storeEvent(event) // One of our children completed
    case event: ArchiveExported => storeEvent(event)
    case event: ArchivalInitiated => // Event comes when one of the children has started archival
    case _: DeleteMessagesSuccess => archivalCompleted() // Event journal no longer contains our events
    case t: Terminated => childActorTerminated(t) // Akka has removed us from memory
    case other => printLogMessage(s"Received message with unknown type. Ignoring it. Message is of type ${other.getClass.getName}")
  }
}
