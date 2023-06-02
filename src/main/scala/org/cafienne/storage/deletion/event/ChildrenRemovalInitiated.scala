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

package org.cafienne.storage.deletion.event

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.message.StorageEvent

import scala.jdk.CollectionConverters.SeqHasAsJava

@Manifest
case class ChildrenRemovalInitiated(metadata: ActorMetadata, members: Seq[ActorMetadata], override val optionalJson: Option[ValueMap] = None) extends StorageEvent {
  override def write(generator: JsonGenerator): Unit = {
    super.writeStorageEvent(generator)
    writeListField(generator, Fields.members, members.asJava)
  }
}

object ChildrenRemovalInitiated {
  def deserialize(json: ValueMap): ChildrenRemovalInitiated = {
    val metadata = ActorMetadata.deserializeMetadata(json)
    val members = ActorMetadata.deserializeChildrenStructure(metadata, json.withArray(Fields.members))
    ChildrenRemovalInitiated(metadata, members, Some(json))
  }
}
