package org.cafienne.storage.actormodel

import akka.actor.ActorRef
import org.cafienne.actormodel.event.ModelEventCollection
import org.cafienne.storage.actormodel.event.ChildrenReceived
import org.cafienne.storage.actormodel.message.{StorageActionCompleted, StorageActionStarted, StorageEvent}

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
