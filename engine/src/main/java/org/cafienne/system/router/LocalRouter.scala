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

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import org.cafienne.actormodel.command.{ModelCommand, TerminateModelActor}
import org.cafienne.actormodel.response.ActorTerminated
import org.cafienne.infrastructure.serialization.DeserializationFailure
import org.cafienne.system.CaseSystem

import scala.collection.mutable

/**
 * In-memory router for messages sent in the CaseSystem.
 * Facilitates actor management in a non-clustered actor system.
 */
class LocalRouter(caseSystem: CaseSystem, actors: mutable.Map[String, ActorRef], terminationRequests: mutable.Map[String, ActorRef]) extends Actor with LazyLogging {
  logger.info(s"Starting case system in local mode, opening router for ${self.path.name}")

  def receive: Actor.Receive = {
    case kill: TerminateModelActor =>
      logger.info(s"Received termination request for actor ${kill.actorId}")
      terminateActor(kill)
    case m: ModelCommand => forwardMessage(m)
    case t: Terminated => removeActorRef(t)
    case d: DeserializationFailure => handleDeserializationFailure(d)
    case other => handleUnknownMessage(other);
  }

  def handleDeserializationFailure(value: DeserializationFailure): Unit = {
    logger.warn(s"The ${getClass.getSimpleName} received a ${value.getClass.getSimpleName} on manifest ${value.manifest}.", value.exception)
  }

  def handleUnknownMessage(value: Any): Unit = {
    logger.warn("The " + getClass.getSimpleName + " received an unknown message of type " + value.getClass.getName + ". Enable debug logging to see the contents of the message")
    logger.whenDebugEnabled(logger.debug("Message:\n", value))
  }

  /**
    * Forward a command to the appropriate ModelActor. Actor will be created if it does not yet exist.
    *
    * @param m
    */
  def forwardMessage(m: ModelCommand): Unit = {
    actors.getOrElseUpdate(m.actorId, createActorRef(m)).forward(m)
  }

  /**
    * Creates the ModelActor that can handle the message and starts watching it
    *
    * @param m
    * @return
    */
  private def createActorRef(m: ModelCommand): ActorRef = {
    // Note: we create the ModelActor as a child to our context
    val ref = context.actorOf(Props.create(m.actorClass, caseSystem), m.actorId)
    // Also start watching the lifecycle of the model actor
    context.watch(ref)
    ref
  }

  def terminateActor(msg: TerminateModelActor): Unit = {
    val actorId = msg.actorId;
    // If the actor is not (or no longer) in memory, We can immediately inform the sender
    actors.get(actorId).fold({
      sender() ! ActorTerminated(actorId)
    })(actor => {
      // Otherwise, store the request, stop the actor and, when the Termination is received, we will inform the requester.
      terminationRequests.put(actorId, sender())
      context.stop(actor)
    })
  }

  /**
    * Clean up the actor ref for the actor that has stopped
    *
    * @param t
    * @return
    */
  def removeActorRef(t: Terminated): Unit = {
    val actorId = t.actor.path.name
    logger.whenDebugEnabled(logger.debug("ModelActor[" + actorId + "] has been terminated. Removing routing reference"))
    if (actors.remove(actorId).isEmpty) {
      logger.warn("Received a Termination message for actor " + actorId + ", but it was not registered in the LocalRoutingService. Termination message is ignored")
    }
    terminationRequests.remove(actorId).foreach(requester => requester ! ActorTerminated(actorId))
  }
}
