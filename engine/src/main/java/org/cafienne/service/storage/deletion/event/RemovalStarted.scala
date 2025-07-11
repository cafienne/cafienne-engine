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

package org.cafienne.service.storage.deletion.event

import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap
import org.cafienne.service.storage.actormodel.ActorMetadata
import org.cafienne.service.storage.actormodel.message.StorageActionStarted

@Manifest
case class RemovalStarted(metadata: ActorMetadata, children: Seq[ActorMetadata], override val optionalJson: Option[ValueMap] = None) extends RemovalEvent with StorageActionStarted

object RemovalStarted {
  def deserialize(json: ValueMap): RemovalStarted = {
    val metadata = ActorMetadata.deserializeMetadata(json)
    val children = ActorMetadata.deserializeChildren(metadata, json.withArray(Fields.children))
    RemovalStarted(metadata, children, Some(json))
  }
}
