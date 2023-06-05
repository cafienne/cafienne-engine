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

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.persistence.journal.Tagged
import akka.persistence.{DeleteMessagesSuccess, RecoveryCompleted}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.{ModelEvent, ModelEventCollection}
import org.cafienne.storage.actormodel.event.StorageRequestReceived
import org.cafienne.storage.actormodel.message._
import org.cafienne.system.CaseSystem

abstract class RootStorageActor(val caseSystem: CaseSystem, val metadata: ActorMetadata) extends BaseStorageActor with ModelEventCollection with LazyLogging {
  /**
    * By choosing the same persistence id, the events of the model actor will also be fed to our recovery protocol,
    * and we are able to delete those events and add our own as well, to keep track of deletion state.
    */
  override def persistenceId: String = self.path.name

  /**
    * Storage type specific initiation event (e.g. RemovalRequested, ArchivalRequested, RestoreRequested)
    *
    * @return
    */
  def createInitialEvent: StorageRequestReceived

  def storageCommand: StorageCommand

  /**
    * The type of actor class that must be instantiated on offspring
    */
  def storageActorType: Class[_ <: Actor]

  def initialEvent: StorageRequestReceived = getEvent(classOf[StorageRequestReceived])

  private def hasRequest: Boolean = optionalEvent(classOf[StorageRequestReceived]).nonEmpty

  private def hasStarted: Boolean = optionalEvent(classOf[StorageActionStarted]).nonEmpty

  private def hasCompletionEvent: Boolean = optionalEvent(classOf[StorageActionCompleted]).nonEmpty

  def addEvent(event: StorageEvent): Unit = events += event

  /**
    * Invoked after the StorageRequest has been fulfilled.
    * It will clean the event journal of the RootStorageActor
    */
  def clearState(): Unit = deleteMessages(Long.MaxValue)

  def getStorageActorRef(metadata: ActorMetadata): ActorRef = getActorRef(metadata, Props(storageActorType, caseSystem, metadata))

  /**
    * Recovery is pretty simple. Simply add all events to our state.
    * This may include StorageEvents, that give information on our current removal state.
    * When recovery completes, ask our state to continue the removal process (but only if it was already active, otherwise wait for first command)
    */
  override def receiveRecover: Receive = {
    case _: RecoveryCompleted => {
      if (hasRequest) {
        printLogMessage("Triggering storage process upon recovery")
        continueStorageProcess()
      }
    } // ... only invoke follow-up logic upon recovery completion with all events present.
    case event: StorageEvent => addEvent(event) // Simply add to the state, and ...
    case event => printLogMessage("Recovering unexpected event of type " + event.getClass.getSimpleName)
  }

  def storeEvent(event: StorageEvent, invokeAfterPersistLogic: => Unit = {}): Unit = {
    // Tag the event, and then after persistence add it to our state and start handling it.
    persist(Tagged(event, Set(ModelEvent.TAG, StorageEvent.TAG)))(_ => {
      addEvent(event)
      invokeAfterPersistLogic
      continueStorageProcess()
    })
  }

  var hasTriggeredStorageActor = false

  def continueStorageProcess(): Unit = {
    if (hasCompletionEvent) {
      completeStorageProcess()
    } else if (hasRequest) {
      if (!hasTriggeredStorageActor) {
        getStorageActorRef(metadata).tell(storageCommand, self)
        hasTriggeredStorageActor = true
      }
    }
  }

  def completeStorageProcess(): Unit = {
    printLogMessage(s"\n\tSTORAGE REQUEST COMPLETED ON $metadata from [$persistenceId]; next step is to clear the state")
    //    println(s"\n\tSTORAGE REQUEST COMPLETED ON $metadata from [$persistenceId]; next step is to clear the state")
    clearState()
  }

  /**
    * When all our events are also removed from the journal we can tell our parent we're done.
    * Also we'll then remove ourselves from memory.
    */
  def journalCleared(): Unit = {
    context.stop(self)
  }

  /**
    * Triggers the storage process on the state directly if the state already
    * has an initiation event, else if will simply add the given event
    * which triggers the storage process in the state.
    *
    * @param event The initiation event to store if one is not yet available
    */
  def triggerStorageProcess(request: StorageCommand, replyTo: ActorRef): Unit = {
    if (hasRequest) {
      printLogMessage(s"Received additional storage request on $persistenceId")
      replyTo ! initialEvent
    } else {
      printLogMessage(s"Received ${request.getClass.getSimpleName} request on $persistenceId")
      storeEvent(createInitialEvent, replyTo ! initialEvent)
    }
  }

  def receiveIncomingMessage(message: Any): Unit = message match {
    case request: StorageCommand => triggerStorageProcess(request, sender())
    case event: StorageEvent => storeEvent(event)
    case t: Terminated => removeActorRef(t)
    case _: DeleteMessagesSuccess => journalCleared() // Event journal no longer contains our persistence id
    case other => logger.warn(s"${this.getClass.getSimpleName} on $metadata received an unknown message of type ${other.getClass.getName}")
  }

  override def receiveCommand: Receive = {
    case message: Any => receiveIncomingMessage(message)
  }

  override def toString: String = s"${getClass.getSimpleName}[$persistenceId] on $metadata"
}
