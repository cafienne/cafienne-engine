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

package org.cafienne.storage

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.{ActorRef, Props}
import org.apache.pekko.util.Timeout
import org.cafienne.actormodel.exception.CommandException
import org.cafienne.storage.actormodel.command.StorageCommand
import org.cafienne.storage.actormodel.message.{StorageEvent, StorageFailure}
import org.cafienne.system.CaseSystem

import scala.concurrent.{ExecutionContext, Future}

class StorageCoordinator(caseSystem: CaseSystem) extends LazyLogging {
  import org.apache.pekko.pattern.ask
  implicit val timeout: Timeout = caseSystem.config.actor.askTimout
  implicit val ec: ExecutionContext = caseSystem.ec

  private val storageCoordinator: ActorRef = caseSystem.system.actorOf(Props(classOf[StorageCoordinationActor], caseSystem))

  def askStorageCoordinator[A <: StorageCommand](command: A)(implicit timeout: Timeout): Future[StorageEvent] = {
    storageCoordinator.ask(command).map {
      case storageEvent: StorageEvent => storageEvent
      case response: StorageFailure =>
        throw new CommandException(response.getMessage)
      case other => // Unknown new type of response that is not handled
        val msg = s"Received an unexpected response after asking ${command.metadata} a command of type ${command.getClass.getSimpleName}. Response is of type ${other.getClass.getSimpleName} - ${other.toString}"
        logger.error(msg)
        throw new CommandException(msg)
    }
  }
}