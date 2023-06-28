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

import akka.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.{ActorMetadata, RootStorageActor}
import org.cafienne.storage.archival.Archive
import org.cafienne.storage.archive.Storage
import org.cafienne.storage.restore.command.RestoreActorData
import org.cafienne.storage.restore.event.{ArchiveRetrieved, RestoreRequested, RestoreStarted}
import org.cafienne.storage.restore.response.ArchiveNotFound
import org.cafienne.system.CaseSystem

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class RootRestorer(caseSystem: CaseSystem, metadata: ActorMetadata) extends RootStorageActor[RestoreNode](caseSystem, metadata) with LazyLogging {
  override def createInitialEvent: RestoreRequested = new RestoreRequested(metadata)

  override def storageActorType: Class[_ <: Actor] = classOf[ActorDataRestorer]

  val state = new RootRestoreState(this)

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
          senderRef ! RestoreStarted(metadata)
        case Failure(throwable) => raiseFailure(throwable)
      }
    } catch {
      case throwable: Throwable => raiseFailure(throwable)
    }
  }

  override def addEvent(event: StorageEvent): Unit = {
    super.addEvent(event)
    event match {
      case event: ArchiveRetrieved => state.createArchiveChain(event)
      case other =>
    }
  }

  override def receiveIncomingMessage(message: Any): Unit = message match {
    case command: RestoreActorData => initiateRestore(command)
    case other => super.receiveIncomingMessage(other)
  }

  override def createOffspringNode(metadata: ActorMetadata): RestoreNode = new RestoreNode(metadata, this)
}

class RootRestoreState(val actor: RootRestorer) extends LazyLogging {
  var archive: Archive = _

  def createArchiveChain(event: ArchiveRetrieved): Unit = {
    def addArchive(archive: Archive): Unit = {
      actor.getNode(archive.metadata).archive = archive.copy(children = Seq()) // No need to carry the archives of the children along
      archive.children.foreach(addArchive) // recursively add archives of all children
    }

    addArchive(event.archive)
  }
}
