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

package org.cafienne.service.storage.archival

import org.cafienne.infrastructure.serialization.{Fields, JacksonSerializable}
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}
import org.cafienne.service.storage.actormodel.ActorMetadata

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class Archive(metadata: ActorMetadata, events: ValueList, children: Seq[Archive] = Seq()) extends JacksonSerializable with CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.metadata, metadata, Fields.events, events, Fields.children, children)

  override def toString: String = toValue.toString
}

object Archive {
  /**
   * Convert a JSON object to an Archive instance
   */
  def deserialize(json: ValueMap): Archive = {
    Archive(
      metadata = ActorMetadata.deserializeMetadata(json),
      events = json.withArray(Fields.events),
      children = json.withArray(Fields.children).getValue.asScala.toSeq.map(_.asInstanceOf[ValueMap]).map(Archive.deserialize)
    )
  }
}
