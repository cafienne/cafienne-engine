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

package org.cafienne.storage.restore

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.DeleteMessagesSuccess
import org.cafienne.infrastructure.Cafienne
import org.cafienne.storage.actormodel.{ActorMetadata, StorageActor}
import org.cafienne.storage.archival.Archive
import org.cafienne.storage.archival.command.RestoreActorData
import org.cafienne.storage.archive.Storage
import org.cafienne.storage.restore.command.RestoreArchive
import org.cafienne.storage.restore.event.{ArchiveRetrieved, ChildRestored, RestoreInitiated}
import org.cafienne.storage.restore.response.ArchiveNotFound
import org.cafienne.system.CaseSystem

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ActorDataRestorer(override val caseSystem: CaseSystem, override val metadata: ActorMetadata) extends StorageActor[RestoreState] {
  override def persistenceId: String = s"RESTORE_ACTOR_FOR_${metadata.actorType.toUpperCase}_${metadata.actorId}"
  override def createState(): RestoreState = new RestoreState(this)

  def initiateRestore(command: RestoreActorData): Unit = {
    //  Use the system dispatcher for handling the export success
    implicit val ec: ExecutionContext = caseSystem.system.dispatcher

    val storage: Storage = Cafienne.config.engine.storage.archive.plugin
    val senderRef = sender()

    def raiseFailure(throwable: Throwable): Unit = {
      logger.warn(s"Cannot find archive ${command.metadata}", throwable)
      senderRef ! ArchiveNotFound(command.metadata)
    }
    try {
      storage.retrieve(command.metadata).onComplete {
        case Success(archive) =>
          self ! ArchiveRetrieved(command.metadata, archive)
          senderRef ! RestoreInitiated(metadata)
        case Failure(throwable) => raiseFailure(throwable)
      }
    } catch {
      case throwable: Throwable => raiseFailure(throwable)
    }
  }

  def getChildActorRef(child: ActorMetadata): ActorRef = {
    children.getOrElseUpdate(child.actorId, {
      // If the child does not yet exist, create it.
      context.watch(context.actorOf(Props(classOf[EventsPersister], caseSystem, child), child.actorId))
    })
  }

  def childActorTerminated(t: Terminated): Unit = {
    val actorId = t.actor.path.name
    if (children.remove(actorId).isEmpty) {
      logger.warn("Received a Termination message for actor " + actorId + ", but it was not registered in the RestoreCoordinator. Termination message is ignored")
    }
  }

  def initiateChildRestore(archive: Archive): Unit = {
    getChildActorRef(archive.metadata).tell(RestoreArchive(archive.metadata, archive), self)
  }

  def completeRestoreProcess(): Unit = {
    printLogMessage("Completed restore process. Deleting ActorDataRestorer state messages for \\\" + self.path\"")
    clearState()
  }

  def restoreCompleted(): Unit = {
    context.stop(self)
  }

  override def receiveCommand: Receive = {
    case command: RestoreActorData => initiateRestore(command) // Initial command. Validate and reply.
    case event: ArchiveRetrieved => storeEvent(event) // Archive available, store and spread.
    case event: ChildRestored => storeEvent(event) // One of our children restored itself, we can remove it from the list.
    case _: DeleteMessagesSuccess => restoreCompleted() // Event journal no longer contains our events, we can be deleted
    case t: Terminated => childActorTerminated(t) // One of our children left memory. That's a good sign...
    case other => logger.warn(s"Received message with unknown type. Ignoring it. Message is of type ${other.getClass.getName}")
  }
}
