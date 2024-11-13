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

package com.casefabric.storage.archival

import org.apache.pekko.persistence.DeleteMessagesSuccess
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.storage.actormodel.message.StorageEvent
import com.casefabric.storage.actormodel.{ActorMetadata, ActorType, QueryDBStorageActor}
import com.casefabric.storage.archival.command.ArchiveActorData
import com.casefabric.storage.archival.event._
import com.casefabric.storage.archival.event.cmmn.ModelActorArchived
import com.casefabric.storage.archival.response.{ArchivalCompleted, ArchivalRejected}
import com.casefabric.storage.archival.state.{ArchivalState, CaseArchivalState, ProcessArchivalState}
import com.casefabric.system.CaseSystem

class ActorDataArchiver(override val caseSystem: CaseSystem, override val metadata: ActorMetadata) extends QueryDBStorageActor[ArchivalState] with LazyLogging {

  printLogMessage(s"\n========== Launching Storage Archival Service ${metadata.path}")

  override def createState(): ArchivalState = metadata.actorType match {
    case ActorType.Case => new CaseArchivalState(this)
    case ActorType.Process => new ProcessArchivalState(this)
    case _ =>
      // TODO: this should send a termination message to the parent context instead of throw an exception
      throw new RuntimeException(s"Cannot handle archival of data on actor type $metadata")
  }

  /**
    * Print a log message to show we're removed from memory.
    */
  override def postStop(): Unit = {
    super.postStop()
    if (metadata.hasParent) {
      printLogMessage(s"========== Finished Storage Archival for $metadata (child of ${metadata.parent.path})\n")
    } else {
      printLogMessage(s"========== Finished Storage Archival for $metadata\n")
    }
  }

  def afterStorageProcessCompleted(): Unit = {
    context.stop(self)
    context.parent ! ArchivalCompleted(metadata)
  }

  /**
    * Invoked when e.g. CaseArchived or ProcessArchived is stored
    */
  def completeStorageProcess(): Unit = {
    // Archival completed; make sure "CaseArchived", etc. exist, and if so, delete the other messages.
    if (state.events.exists(_.isInstanceOf[ModelActorArchived])) {
      clearState(lastSequenceNr - 1)
    }
  }

  def afterArchiveExported(): Unit = {
    printLogMessage("Found acknowledgement that our archive has been exported")
    storeEvent(state.createModelActorStorageEvent)
  }

  def afterArchiveCreated(event: ArchiveCreated): Unit = {
    context.parent ! event
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
  def startStorageProcess(command: ArchiveActorData): Unit = {
    printLogMessage("Received command to archive myself")
    if (lastSequenceNr == 0) {
      printLogMessage("Actor has not recovered any events. Probably does not exist at all")
      sender() ! ArchivalRejected(command.metadata, "Actor does not exist in the event journal")
    } else if (state.events.nonEmpty && !state.hasExpectedEvents) {
      printLogMessage(s"State does not match expected actor type $metadata; state contains: ${state.actualModelActorType}")
      sender() ! ArchivalRejected(command.metadata, s"Expected actor $metadata; Found: ${state.actualModelActorType}")
    } else {
      if (state.isCleared) {
        // No need to do anything, as our parent is informed and we can simply go offline again
        completeStorageProcess()
      } else if (state.parentReceivedArchive) {
        printLogMessage("Our parent is aware that we are archived, but we have not yet cleaned up ourself, doing that now")
        afterArchiveExported()
      } else if (state.isCreated) {
        afterArchiveCreated(state.archive)
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
    case command: ArchiveActorData => startStorageProcess(command) // Initial command. Validate and reply.
    case event: StorageEvent => storeEvent(event) // We now know which children to remove
    case _: DeleteMessagesSuccess => afterStorageProcessCompleted() // Event journal no longer contains our events
    case other => reportUnknownMessage(other)
  }
}
