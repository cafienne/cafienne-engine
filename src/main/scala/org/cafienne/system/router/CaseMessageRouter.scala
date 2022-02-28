/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.system.router

import akka.actor.{Actor, Terminated}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.{ModelCommand, TerminateModelActor}

/**
  * Base class for routing model commands into the case system
  */
abstract class CaseMessageRouter extends Actor with LazyLogging {
  def receive: Actor.Receive = {
    case kill: TerminateModelActor =>
      logger.info(s"Received termination request for actor ${kill.actorId}")
      terminateActor(kill.actorId)
    case m: ModelCommand => forwardMessage(m)
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

  def forwardMessage(m: ModelCommand): Unit

  def terminateActor(str: String): Unit
}
