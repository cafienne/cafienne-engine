/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.system.router

import akka.actor.{Actor, ActorPath, InvalidActorNameException, Terminated}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.response.CommandFailure
import org.cafienne.actormodel.command.{ModelCommand, TerminateModelActor}

/**
  * Base class for routing model commands into the case system
  */
abstract class CaseMessageRouter extends Actor with LazyLogging {
  def receive: Actor.Receive = {
    case kill: TerminateModelActor => {
      logger.info(s"Received termination request for actor ${kill.actorId}")
      terminateActor(kill.actorId)
    }
    case m: ModelCommand[_] => forwardMessage(m)
    case t: Terminated => removeActorRef(t)
    case other => handleUnknownMessage(other);
  }

  def removeActorRef(terminated: Terminated): Unit = {
    logger.warn("Case Message Router of type " + getClass.getName + " unexpectedly received a Terminated message: " + terminated)
  }

  def handleUnknownMessage(value: Any): Unit = {
    logger.warn("The " + getClass.getSimpleName + " received an unknown message of type " + value.getClass.getName + ". Enable debug logging to see the contents of the message")
    logger.debug("Message:\n", value)
  }

  /**
    * Checks the actor id inside the model command, and returns an error if it is not valid.
    * Upon error, the forwardMessage() method will not be invoked.
    * @param m
    */
  def verifyCommand(m: ModelCommand[_]): Unit = {
    try {
      // Try and validate the actor path. May result in an exception, and then we do not forward the message into the cluster system
      ActorPath.validatePathElement(m.actorId)
      // Reaching this point means we have an "akka-valid" actor id, let's forward the message to matching shard
      forwardMessage(m)
    } catch {
      // If something fails in sending the message (typically creation of actorRef failed), then we will respond back with a message
      case t: Throwable => {
        val deepInfo = t match {
          case i: InvalidActorNameException => "Invalid identifier in message of type " + m.getClass.getSimpleName + "\n" + i.message
          case other => {
            // This is a pretty weird situation; right now no idea why this would happened, but still logging.
            logger.error("Unexpected routing failure in handling command of type " + m.getClass.getName + ": " + m.toJson, other)
            "Could not send message of type " + m.getClass.getSimpleName + " into the case system. An exception of type " + other.getClass.getName + " happened. Check the engine logs for more information."
          }
        }
        // Create a response message that will go all the way back to the client.
        val failure = new CommandFailure(m, new IllegalArgumentException(deepInfo))
        context.sender().tell(failure, context.self)
      }
    }
  }

  def forwardMessage(m: ModelCommand[_]): Unit

  def terminateActor(str: String): Unit
}
