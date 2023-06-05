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
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.{ActorMetadata, StorageActor}
import org.cafienne.storage.archival.Archive
import org.cafienne.storage.archive.Storage
import org.cafienne.storage.restore.command.{RestoreActorData, RestoreArchive}
import org.cafienne.storage.restore.event.{ArchiveRetrieved, RestoreInitiated}
import org.cafienne.storage.restore.response.ArchiveNotFound
import org.cafienne.system.CaseSystem

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ActorDataRestorer(override val caseSystem: CaseSystem, override val metadata: ActorMetadata) extends StorageActor[RestoreState] {
  override def persistenceId: String = s"RESTORE_ACTOR_FOR_${metadata.actorType.toUpperCase}_${metadata.actorId}"
  override def createState(): RestoreState = new RestoreState(this)

  def startStorageProcess(command: RestoreActorData): Unit = {
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
    getActorRef(child, Props(classOf[EventsPersister], caseSystem, child))
  }

  def initiateChildRestore(archive: Archive): Unit = {
    getChildActorRef(archive.metadata).tell(RestoreArchive(archive.metadata, archive), self)
  }

  def completeStorageProcess(): Unit = {
    printLogMessage("Completed restore process. Deleting ActorDataRestorer state messages for \\\" + self.path\"")
    clearState()
  }

  def afterStorageProcessCompleted(): Unit = {
    context.stop(self)
  }

  override def receiveCommand: Receive = {
    case command: RestoreActorData => startStorageProcess(command) // Initial command. Validate and reply.
    case event: StorageEvent => storeEvent(event) // We now know which children to remove
    case _: DeleteMessagesSuccess => afterStorageProcessCompleted() // Event journal no longer contains our events, we can be deleted
    case t: Terminated => removeActorRef(t) // One of our children left memory. That's a good sign...
    case other => reportUnknownMessage(other)
  }
}
