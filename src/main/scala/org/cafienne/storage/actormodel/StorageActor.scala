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

package org.cafienne.storage.actormodel

import akka.persistence.journal.Tagged
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.storage.actormodel.message.{StorageActionInitiated, StorageCommand, StorageEvent}
import org.cafienne.storage.actormodel.state.StorageActorState
import org.cafienne.system.CaseSystem

trait StorageActor[S <: StorageActorState]
  extends PersistentActor
    with StorageActorSupervisor
    with LazyLogging {
  val caseSystem: CaseSystem
  val metadata: ActorMetadata

  /**
   * By choosing the same persistence id, the events of the model actor will also be fed to our recovery protocol,
   * and we are able to delete those events and add our own as well, to keep track of deletion state.
   */
  override def persistenceId: String = metadata.actorId

  def createState(): S

  /**
    * Invoked after the StorageActor has done it's job.
    * This can be used to clean the storage job state.
    * @param toSequenceNr Up to which event the journal must be cleared
    */
  def clearState(toSequenceNr: Long = Long.MaxValue): Unit = {
    deleteMessages(toSequenceNr)
  }

  /**
   * Every type of ModelActor that we operate on has a specific state.
   */
  val state: S = createState()

  def printLogMessage(msg: String): Unit = {
    Printer.print(this.metadata, msg)
  }

  /**
   * Recovery is pretty simple. Simply add all events to our state.
   * This may include StorageEvents, that give information on our current removal state.
   * When recovery completes, ask our state to continue the removal process (but only if it was already active, otherwise wait for first command)
   */
  override def receiveRecover: Receive = {
    case _: RecoveryCompleted => state.handleRecoveryCompletion() // ... only invoke follow-up logic upon recovery completion with all events present.
    case event: ModelEvent => state.addEvent(event) // Simply add to the state, and ...
    case event => printLogMessage("Recovering unexpected event of type " + event.getClass.getSimpleName)
  }

  def storeEvent(event: StorageEvent): Unit = {
    // Tag the event, and then after persistence add it to our state and start handling it.
    persist(Tagged(event, Set(ModelEvent.TAG, StorageEvent.TAG)))(_ => state.addEvent(event))
  }

  /**
    * Triggers the storage process on the state directly if the state already
    * has an initiation event, else if will simply add the given event
    * which triggers the storage process in the state.
    * @param event The initiation event to store if one is not yet available
    */
  def triggerStorageProcess(command: StorageCommand, event: StorageActionInitiated): Unit = {
    if (state.hasInitiationEvent) {
      state.continueStorageProcess()
    } else {
      state.addEvent(event)
    }
    // Inform the sender of the command about the event
    sender() ! event
  }
}

/**
 * Simplistic console printer. To be replaced with proper log infrastructure (whenDebugEnabled and such)
 */
object Printer extends LazyLogging {
  var previousActor: ActorMetadata = _

  def out(msg: String): Unit = {
    logger.whenDebugEnabled(logger.debug(msg))
  }

  def print(metadata: ActorMetadata, msg: String): Unit = {
    val path = if (metadata == previousActor) "- " else {
      val root = if (metadata.isRoot) "ROOT " else ""
      "\n" + root + metadata.path + ":\n- "
    }

    if (msg.startsWith("\n====") || msg.startsWith("====")) {
      out(msg)
    } else if (msg.startsWith("\n")) {
      out(s"\n$path${msg.substring(1)}")
    } else {
      out(s"$path$msg")
    }
    previousActor = metadata
  }
}
