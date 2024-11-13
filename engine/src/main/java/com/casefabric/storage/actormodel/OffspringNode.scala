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

package com.casefabric.storage.actormodel

import org.apache.pekko.actor.ActorRef
import com.casefabric.actormodel.event.ModelEventCollection
import com.casefabric.storage.actormodel.event.ChildrenReceived
import com.casefabric.storage.actormodel.message.{StorageActionCompleted, StorageActionStarted, StorageEvent}

trait OffspringNode extends ModelEventCollection {
  val metadata: ActorMetadata
  val actor: RootStorageActor[_]

  final def isRoot: Boolean = this == actor.rootNode

  lazy val storageActor: ActorRef = actor.getStorageActorRef(metadata)

  def terminateModelActor(metadata: ActorMetadata, followUpAction: => Unit = {}): Unit = actor.terminateModelActor(metadata, followUpAction)

  def createStorageCommand: Any

  def informActor(message: Any): Unit = storageActor.tell(message, actor.self)

  override def toString: String = s"${getClass.getSimpleName} on $metadata"

  def printLogMessage(msg: String): Unit = actor.printLogMessage(msg)

  def hasStarted: Boolean = events.exists(_.isInstanceOf[StorageActionStarted])

  def hasCompletionEvent: Boolean = events.exists(_.isInstanceOf[StorageActionCompleted])

  def hasCompleted: Boolean = hasCompletionEvent

  def addOffspring(event: StorageActionStarted): Unit = event.children.foreach(actor.getNode)

  def addEvent(event: StorageEvent): Unit = {
    events += event
    updateState(event)
    if (actor.recoveryFinished) {
      uponReceiveEvent(event)
    }
  }

  protected def updateState(event: StorageEvent): Unit = event match {
    case event: StorageActionStarted => addOffspring(event)
    case _ => // no followup needed
  }

  protected def uponReceiveEvent(event: StorageEvent): Unit = event match {
    case event: StorageActionStarted => informActor(ChildrenReceived(event.metadata))  // Tell the remote actor we've successfully received the children
    case _ => // no followup needed
  }

  private var startedStorageProcess: Boolean = false

  def startStorageProcess(): Unit = {
    if (!startedStorageProcess) {
      terminateModelActor(metadata, informActor(createStorageCommand))
      startedStorageProcess = true
    }
  }

  def continueStorageProcess(): Unit = {
    if (!hasStarted) {
      startStorageProcess()
    }
  }
}
