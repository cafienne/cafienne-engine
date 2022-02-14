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

package org.cafienne.storage.archival

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}
import org.cafienne.storage.actormodel.message.StorageSerializable

case class Archive(events: ValueMap) extends StorageSerializable with CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.events, events)
}

object Archive {
  /**
   * Convert a JSON object to an Archive instance
   */
  def deserialize(json: ValueMap): Archive = {
    Archive(events = json.readMap(Fields.events))
  }
}
