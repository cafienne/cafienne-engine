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

package org.cafienne.service.storage.actormodel

import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import org.apache.pekko.persistence.journal.Tagged
import org.apache.pekko.persistence.{DeleteMessagesSuccess, RecoveryCompleted}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.service.storage.actormodel.command.StorageCommand
import org.cafienne.service.storage.actormodel.event.StorageRequestReceived
import org.cafienne.service.storage.actormodel.message._
import org.cafienne.system.CaseSystem

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class RootStorageActor[O <: OffspringNode](val caseSystem: CaseSystem, val metadata: ActorMetadata) extends BaseStorageActor with LazyLogging {
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

  /**
    * The type of actor class that must be instantiated on offspring
    */
  def storageActorType: Class[_ <: Actor]

  val events: ListBuffer[StorageEvent] = ListBuffer()
  var initialEvent: StorageRequestReceived = _

  val nodes: mutable.Map[String, O] = mutable.HashMap[String, O]()
  val rootNode: O = getNode(metadata)

  def getParent(node: O): Option[O] = {
    val parent = node.metadata.parent
    if (parent != null) {
      nodes.get(parent.actorId)
    } else {
      None
    }
  }

  def getNode(metadata: ActorMetadata): O = nodes.getOrElseUpdate(metadata.actorId, createOffspringNode(metadata))

  def getChildren(parent: O): Seq[O] = nodes.values.filter(child => child.metadata.parent != null && child.metadata.parent.actorId == parent.metadata.actorId).toSeq

  def createOffspringNode(metadata: ActorMetadata): O

  private def hasRequest: Boolean = initialEvent != null

  def addEvent(event: StorageEvent): Unit = {
    events += event
    event match {
      case event: StorageRequestReceived => initialEvent = event
      case other => getNode(event.metadata).addEvent(other)
    }
  }

  override def postStop(): Unit = {
    printLogMessage("Stopped " + this)
  }

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

  def continueStorageProcess(): Unit = {
    val incompleteNodes = nodes.values.filterNot(_.hasCompleted).toSeq
    if (incompleteNodes.nonEmpty) {
//      println(s"$this: found ${incompleteNodes.size} incomplete nodes out of total ${nodes.size} nodes")
      incompleteNodes.foreach(_.continueStorageProcess())
    } else {
      // We've completed the storage request!
      printLogMessage(s"Completed storage process on $rootNode")
      completeStorageProcess()
    }
  }

  def completeStorageProcess(): Unit = {
    printLogMessage(s"\n\tSTORAGE REQUEST COMPLETED ON $metadata from [$persistenceId]; next step is to clear the state")
//    println(s"\n\tSTORAGE REQUEST COMPLETED ON $metadata from [$persistenceId]; next step is to clear the state")
    clearState()
   }

  /**
   * Invoked after the StorageRequest has been fulfilled.
   * It will clean the event journal of the RootStorageActor
   */
  def clearState(): Unit = deleteMessages(lastSequenceNr)

  private var deletionCount = 0L
  /**
    * When all our events are also removed from the journal we can tell our parent we're done.
    * Also we'll then remove ourselves from memory.
    */
  private def afterMessagesDeleted(e: DeleteMessagesSuccess): Unit = {
    if (e.toSequenceNr >= lastSequenceNr) {
      if (deletionCount > 0) {
        printLogMessage("Completely cleared journal for " + metadata +" with e: " + e.toSequenceNr +" and last sequence number: " + lastSequenceNr)
      }
      deletionCount = e.toSequenceNr
      context.stop(self)
    } else {
      deletionCount = e.toSequenceNr
      printLogMessage("Cleared journal for " + metadata +" with up to message number " + e.toSequenceNr +" and last sequence number: " + lastSequenceNr +" current deletion count: " + deletionCount)
    }
  }

  /**
    * Triggers the storage process on the state directly if the state already
    * has an initiation event, else if will simply add the given event
    * which triggers the storage process in the state.
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
    case e: DeleteMessagesSuccess => afterMessagesDeleted(e) // Event journal no longer contains our persistence id
    case other => logger.warn(s"${this.getClass.getSimpleName} on $metadata received an unknown message of type ${other.getClass.getName}")
  }

  override def receiveCommand: Receive = {
    case message: Any => receiveIncomingMessage(message)
  }

  override def toString: String = s"${getClass.getSimpleName}[$persistenceId] on $metadata"
}
