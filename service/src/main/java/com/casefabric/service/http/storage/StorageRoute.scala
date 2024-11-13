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

package com.casefabric.service.http.storage

import org.apache.pekko.actor.{ActorRef, ActorSystem, Props}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.{complete, onComplete}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.service.infrastructure.route.AuthenticatedRoute
import com.casefabric.storage.StorageCoordinator
import com.casefabric.storage.actormodel.ActorMetadata
import com.casefabric.storage.actormodel.event.StorageRequestReceived
import com.casefabric.storage.actormodel.message.{StorageActionRejected, StorageActionStarted, StorageCommand, StorageFailure}
import com.casefabric.storage.archival.command.ArchiveActorData
import com.casefabric.storage.deletion.command.RemoveActorData
import com.casefabric.storage.deletion.event.RemovalCompleted
import com.casefabric.storage.restore.command.RestoreActorData
import com.casefabric.system.CaseSystem

import scala.util.{Failure, Success}

trait StorageRoute extends AuthenticatedRoute {
  // Start a singleton coordinator.
  //  The coordinator will also open the event stream on current events to recover existing
  //  deletion processes that have not yet finished.
  StorageRoute.startCoordinator(caseSystem)

  def initiateDataRemoval(metadata: ActorMetadata): Route = {
    StorageRoute.askStorageCoordinator(RemoveActorData(metadata))
  }

  def initiateDataArchival(metadata: ActorMetadata): Route = {
    StorageRoute.askStorageCoordinator(ArchiveActorData(metadata))
  }

  def restoreActorData(metadata: ActorMetadata): Route = {
    StorageRoute.askStorageCoordinator(RestoreActorData(metadata))
  }
}

object StorageRoute extends LazyLogging {
  private var storageCoordinator: ActorRef = _ // Will be initialized as soon as a StorageRoute is loaded

  def startCoordinator(caseSystem: CaseSystem): Unit = {
    if (storageCoordinator == null) {
      val system: ActorSystem = caseSystem.system
      storageCoordinator = system.actorOf(Props(classOf[StorageCoordinator], caseSystem))
    }
  }

  def askStorageCoordinator(command: StorageCommand): Route = {
    import org.apache.pekko.pattern.ask
    implicit val timeout: Timeout = CaseFabric.config.actor.askTimout

    onComplete(storageCoordinator.ask(command)) {
      case Success(value) =>
        value match {
          case _: StorageRequestReceived =>
            complete(StatusCodes.Accepted)
          case _: StorageActionStarted =>
            complete(StatusCodes.Accepted)
          case rejection: StorageActionRejected =>
            logger.error(s"Removal of ${command.metadata} is rejected with reason ${rejection.msg}")
            complete(StatusCodes.NotFound)
          case _: RemovalCompleted =>
            complete(StatusCodes.NotFound, s"Cannot find ${command.metadata}")
          case response: StorageFailure =>
            complete(StatusCodes.NotFound, response.getMessage)
          case other => // Unknown new type of response that is not handled
            logger.error(s"Received an unexpected response after asking ${command.metadata} a command of type ${command.getClass.getSimpleName}. Response is of type ${other.getClass.getSimpleName}")
            complete(StatusCodes.Accepted)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }
}