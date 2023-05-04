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

package org.cafienne.system.router

import akka.actor.{Actor, Terminated}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.{ModelCommand, TerminateModelActor}
import org.cafienne.infrastructure.serialization.DeserializationFailure

/**
  * Base class for routing model commands into the case system
  */
abstract class CaseMessageRouter extends Actor with LazyLogging {
  def receive: Actor.Receive = {
    case kill: TerminateModelActor =>
      logger.info(s"Received termination request for actor ${kill.actorId}")
      terminateActor(kill)
    case m: ModelCommand => forwardMessage(m)
    case t: Terminated => removeActorRef(t)
    case d: DeserializationFailure => handleDeserializationFailure(d)
    case other => handleUnknownMessage(other);
  }

  def removeActorRef(terminated: Terminated): Unit

  def forwardMessage(m: ModelCommand): Unit

  def terminateActor(msg: TerminateModelActor): Unit

  def handleDeserializationFailure(value: DeserializationFailure): Unit = {
    logger.warn(s"The ${getClass.getSimpleName} received a ${value.getClass.getSimpleName} on manifest ${value.manifest}.", value.exception)
  }

  def handleUnknownMessage(value: Any): Unit = {
    logger.warn("The " + getClass.getSimpleName + " received an unknown message of type " + value.getClass.getName + ". Enable debug logging to see the contents of the message")
    logger.debug("Message:\n", value)
  }
}
