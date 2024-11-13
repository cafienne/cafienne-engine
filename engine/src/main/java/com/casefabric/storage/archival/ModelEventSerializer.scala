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

package com.casefabric.storage.archival

import com.casefabric.actormodel.event.ModelEvent
import com.casefabric.infrastructure.serialization.{CaseFabricSerializer, Fields}
import com.casefabric.json.ValueMap

object ModelEventSerializer {

  // Write a ModelEvent and it's sequenceNr to JSON
  def serializeEventToJson(event: ModelEvent, sequenceNr: Long): ValueMap = {
    new ValueMap(Fields.sequenceNr, sequenceNr, Fields.manifest, CaseFabricSerializer.getManifestString(event), Fields.content, event.rawJson())
  }

  def deserializeEvent(json: ValueMap): AnyRef = {
    val manifest = json.readString(Fields.manifest)
    val content = json.readMap(Fields.content)
    new CaseFabricSerializer().fromJson(content, manifest)
  }
}
