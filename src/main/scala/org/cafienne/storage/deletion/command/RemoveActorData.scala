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

package org.cafienne.storage.deletion.command

import org.cafienne.storage.actormodel.{ActorMetadata, StorageActor}
import org.cafienne.storage.actormodel.message.StorageCommand
import org.cafienne.storage.deletion.ActorDataRemover

/** @param user User initiating the removal process
  * @param tenant Tenant to which the ModelActor belongs
  * @param actorId Persistence id of the ModelActor (e.g., tenant name, case instance id, process task id, consent group id)
  * @param parentActorId Optional owner of the parent actor that triggered the remove (e.g., a process task removal is initiated by it's parent case)
  */
case class RemoveActorData(metadata: ActorMetadata) extends StorageCommand {
  override def toString: String = s"RemovalCommand for $metadata"
  val actorClass: Class[_ <: StorageActor[_]] = classOf[ActorDataRemover]
}
