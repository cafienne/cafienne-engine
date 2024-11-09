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

import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.serialization.{CafienneSerializer, Fields}
import org.cafienne.json.ValueMap

object ModelEventSerializer {

  // Write a ModelEvent and it's sequenceNr to JSON
  def serializeEventToJson(event: ModelEvent, sequenceNr: Long): ValueMap = {
    new ValueMap(Fields.sequenceNr, sequenceNr, Fields.manifest, CafienneSerializer.getManifestString(event), Fields.content, event.rawJson())
  }

  def deserializeEvent(json: ValueMap): AnyRef = {
    val manifest = json.readString(Fields.manifest)
    val content = json.readMap(Fields.content)
    new CafienneSerializer().fromJson(content, manifest)
  }
}
