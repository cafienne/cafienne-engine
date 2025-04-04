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

package org.cafienne.service.http.storage

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.util.Timeout
import org.cafienne.service.infrastructure.route.AuthenticatedRoute
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.command.StorageCommand
import org.cafienne.storage.actormodel.event.StorageRequestReceived
import org.cafienne.storage.actormodel.message.{StorageActionRejected, StorageActionStarted}
import org.cafienne.storage.archival.command.ArchiveActorData
import org.cafienne.storage.deletion.command.RemoveActorData
import org.cafienne.storage.deletion.event.RemovalCompleted
import org.cafienne.storage.restore.command.RestoreActorData

import scala.util.{Failure, Success}

trait StorageRoute extends AuthenticatedRoute {
  implicit val timeout: Timeout = caseSystem.config.actor.askTimout

  def initiateDataRemoval(metadata: ActorMetadata): Route = {
    askStorageCoordinator(RemoveActorData(metadata))
  }

  def initiateDataArchival(metadata: ActorMetadata): Route = {
    askStorageCoordinator(ArchiveActorData(metadata))
  }

  def restoreActorData(metadata: ActorMetadata): Route = {
    askStorageCoordinator(RestoreActorData(metadata))
  }

  private def askStorageCoordinator(command: StorageCommand)(implicit timeout: Timeout): Route = {
    onComplete(caseSystem.storageCoordinator.askStorageCoordinator(command)) {
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
          case other => // Unknown new type of response that is not handled
            logger.error(s"Received an unexpected response after asking ${command.metadata} a command of type ${command.getClass.getSimpleName}. Response is of type ${other.getClass.getSimpleName}")
            complete(StatusCodes.Accepted)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }

}
