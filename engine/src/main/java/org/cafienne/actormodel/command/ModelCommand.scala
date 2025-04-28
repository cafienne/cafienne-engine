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
package org.cafienne.actormodel.command

import org.cafienne.actormodel.ModelActor
import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.actormodel.message.IncomingActorMessage
import org.cafienne.actormodel.response.ModelResponse

trait ModelCommand extends IncomingActorMessage with RootCaseIdentifiable {
  /**
    * Returns the user context for this command.
    */
  override def getUser: UserIdentity

  /**
    * Returns a string with the identifier of the actor towards this command must be sent.
    */
  def getActorId: String

  def actorId: String = getActorId

  /**
    * Return the actor handling this command. May return null if setActor() is not yet invoked.
    */
  def getActor: ModelActor

  /**
    * Through this method, the command is made aware of the actor that is handling it.
    */
  def setActor(actor: ModelActor): Unit

  def validateCommand(actor: ModelActor): Unit

  def processCommand(actor: ModelActor): Unit

  def getResponse: ModelResponse

  def getCommandDescription: String = getDescription

  override def isCommand = true

  override def asCommand: ModelCommand = this
}